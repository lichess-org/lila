package lila.round

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.{ Color, White, Black }
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.chat.Chat
import lila.common.LightUser
import lila.game.actorApi.{ StartGame, UserStartGame }
import lila.game.{ Game, GameRepo, Event }
import lila.hub.actorApi.Deploy
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.round.{ IsOnGame, TourStanding }
import lila.hub.actorApi.tv.{ Select => TvSelect }
import lila.hub.TimeBomb
import lila.socket._
import lila.socket.actorApi.{ Connected => _, _ }
import makeTimeout.short

private[round] final class Socket(
    gameId: Game.ID,
    history: History,
    lightUser: LightUser.Getter,
    uidTimeout: FiniteDuration,
    socketTimeout: FiniteDuration,
    disconnectTimeout: FiniteDuration,
    ragequitTimeout: FiniteDuration,
    simulActor: ActorSelection
) extends SocketActor[Member](uidTimeout) {

  private var hasAi = false
  private var mightBeSimul = true // until proven false
  private var chatIds = Socket.ChatIds(
    priv = Chat.Id(gameId), // until replaced with tourney/simul chat
    pub = Chat.Id(s"$gameId/w")
  )
  private var tournamentId = none[String] // until set, to listen to standings

  private val timeBomb = new TimeBomb(socketTimeout)

  private var delayedCrowdNotification = false

  private final class Player(color: Color) {

    // when the player has been seen online for the last time
    private var time: Long = nowMillis
    // wether the player closed the window intentionally
    private var bye: Int = 0

    var userId = none[String]

    def ping: Unit = {
      isGone foreach { _ ?? notifyGone(color, false) }
      if (bye > 0) bye = bye - 1
      time = nowMillis
    }
    def setBye: Unit = {
      bye = 3
    }
    private def isBye = bye > 0

    private def isHostingSimul: Fu[Boolean] = userId.ifTrue(mightBeSimul) ?? { u =>
      simulActor ? lila.hub.actorApi.simul.GetHostIds mapTo manifest[Set[String]] map (_ contains u)
    }

    def isGone: Fu[Boolean] =
      (time < (nowMillis - isBye.fold(ragequitTimeout, disconnectTimeout).toMillis)) ?? !isHostingSimul

    def isOnline = time > nowMillis - uidTimeout.toMillis
  }

  private val whitePlayer = new Player(White)
  private val blackPlayer = new Player(Black)

  override def preStart(): Unit = {
    super.preStart()
    buscriptions.all
    GameRepo game gameId map SetGame.apply pipeTo self
  }

  override def postStop(): Unit = {
    super.postStop()
    lilaBus.unsubscribe(self)
  }

  private object buscriptions {

    def all = {
      tv
      chat
      tournament
    }

    def tv = members.flatMap { case (_, m) => m.userTv }.toSet foreach { (userId: String) =>
      lilaBus.subscribe(self, Symbol(s"userStartGame:$userId"))
    }

    def chat = lilaBus.subscribe(self, Symbol(s"chat:${chatIds.priv}"), Symbol(s"chat:${chatIds.pub}"))

    def tournament = tournamentId foreach { id =>
      lilaBus.subscribe(self, Symbol(s"tour-standing-$id"))
    }
  }

  def receiveSpecific = ({

    case SetGame(Some(game)) =>
      hasAi = game.hasAi
      whitePlayer.userId = game.player(White).userId
      blackPlayer.userId = game.player(Black).userId
      mightBeSimul = game.isSimul
      game.tournamentId orElse game.simulId map Chat.Id.apply foreach { chatId =>
        chatIds = chatIds.copy(priv = chatId)
        buscriptions.chat
      }
      game.tournamentId foreach { tourId =>
        tournamentId = tourId.some
        buscriptions.tournament
      }
    case SetGame(None) => self ! PoisonPill // should never happen but eh

    // from lilaBus 'startGame
    // sets definitive user ids
    // in case one joined after the socket creation
    case StartGame(game) => self ! SetGame(game.some)

    case d: Deploy =>
      onDeploy(d)
      history.enablePersistence

    case Ping(uid, Some(v), c) =>
      timeBomb.delay
      ping(uid, c)
      ownerOf(uid) foreach { o =>
        playerDo(o.color, _.ping)
      }
      withMember(uid) { member =>
        (history getEventsSince v).fold(resyncNow(member))(batch(member, _))
      }

    case BotPing(color) =>
      playerDo(color, _.ping)
      notifyCrowd

    case Bye(color) => playerDo(color, _.setBye)

    case Broom =>
      broom
      if (timeBomb.boom) self ! PoisonPill
      else if (!hasAi) Color.all foreach { c =>
        playerGet(c, _.isGone) foreach { _ ?? notifyGone(c, true) }
      }

    case IsGone(color) => playerGet(color, _.isGone) pipeTo sender

    case IsOnGame(color) => sender ! ownerIsHere(color)

    case GetSocketStatus =>
      playerGet(White, _.isGone) zip playerGet(Black, _.isGone) map {
        case (whiteIsGone, blackIsGone) => SocketStatus(
          version = history.getVersion,
          whiteOnGame = ownerIsHere(White),
          whiteIsGone = whiteIsGone,
          blackOnGame = ownerIsHere(Black),
          blackIsGone = blackIsGone
        )
      } pipeTo sender

    case Join(uid, user, color, playerId, ip, onTv) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, color, playerId, ip, onTv)
      addMember(uid.value, member)
      notifyCrowd
      if (playerId.isDefined) playerDo(color, _.ping)
      sender ! Connected(enumerator, member)
      if (member.userTv.isDefined) buscriptions.tv

    case Nil =>
    case eventList: EventList => notify(eventList.events)

    case lila.chat.actorApi.ChatLine(chatId, line) => notify(List(line match {
      case l: lila.chat.UserLine => Event.UserMessage(l, chatId == chatIds.pub)
      case l: lila.chat.PlayerLine => Event.PlayerMessage(l)
    }))

    case a: lila.analyse.actorApi.AnalysisProgress =>
      import lila.analyse.{ JsonView => analysisJson }
      notifyAll("analysisProgress", Json.obj(
        "analysis" -> analysisJson.bothPlayers(a.game, a.analysis),
        "tree" -> TreeBuilder(
          id = a.analysis.id,
          pgnMoves = a.game.pgnMoves,
          variant = a.variant,
          analysis = a.analysis.some,
          initialFen = a.initialFen,
          withFlags = JsonView.WithFlags(),
          clocks = none
        )
      ))

    case ChangeFeatured(_, msg) => foreachWatcher(_ push msg)

    case TvSelect(msg) => foreachWatcher(_ push msg)

    case UserStartGame(userId, game) => foreachWatcher { m =>
      if (m.onUserTv(userId) && !m.userId.exists(game.userIds.contains))
        m push makeMessage("resync")
    }

    case NotifyCrowd =>
      delayedCrowdNotification = false
      showSpectators(lightUser)(members.values.filter(_.watcher)) foreach { spectators =>
        val event = Event.Crowd(
          white = ownerIsHere(White),
          black = ownerIsHere(Black),
          watchers = spectators
        )
        notifyAll(event.typ, event.data)
      }

    case TourStanding(json) => notifyOwners("tourStanding", json)

  }: Actor.Receive) orElse lila.chat.Socket.out(
    send = (t, d, _) => notifyAll(t, d)
  )

  override def quit(uid: String) = {
    members get uid foreach { member =>
      super.quit(uid)
      notifyCrowd
    }
  }

  def notifyCrowd: Unit = {
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      context.system.scheduler.scheduleOnce(1 second, self, NotifyCrowd)
    }
  }

  def notify(events: Events): Unit = {
    val vevents = history addEvents events
    members.foreachValue { m => batch(m, vevents) }
  }

  def batch(member: Member, vevents: List[VersionedEvent]) = vevents match {
    case Nil =>
    case List(one) => member push one.jsFor(member)
    case many => member push makeMessage("b", many map (_ jsFor member))
  }

  def notifyOwner[A: Writes](color: Color, t: String, data: A) =
    withOwnerOf(color) {
      _ push makeMessage(t, data)
    }

  def notifyGone(color: Color, gone: Boolean): Unit = {
    notifyOwner(!color, "gone", gone)
  }

  def withOwnerOf(color: Color)(f: Member => Unit) =
    members.foreachValue { m =>
      if (m.owner && m.color == color) f(m)
    }

  def notifyOwners[A: Writes](t: String, data: A) =
    members.foreachValue { m =>
      if (m.owner) m push makeMessage(t, data)
    }

  def ownerIsHere(color: Color) = playerGet(color, _.isOnline)

  def ownerOf(uid: String): Option[Member] =
    members get uid filter (_.owner)

  def foreachWatcher(f: Member => Unit): Unit = members.foreachValue { m =>
    if (m.watcher) f(m)
  }

  private def playerGet[A](color: Color, getter: Player => A): A =
    getter(color.fold(whitePlayer, blackPlayer))

  private def playerDo(color: Color, effect: Player => Unit): Unit = {
    effect(color.fold(whitePlayer, blackPlayer))
  }
}

object Socket {

  case class ChatIds(priv: Chat.Id, pub: Chat.Id) {
    def update(g: Game) =
      g.tournamentId.map { id => copy(priv = Chat.Id(id)) } orElse
        g.simulId.map { id => copy(priv = Chat.Id(id)) } getOrElse
        this
  }
}
