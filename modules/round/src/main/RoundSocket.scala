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
import lila.hub.actorApi.simul.GetHostIds
import lila.hub.actorApi.tv.{ Select => TvSelect }
import lila.hub.Trouper
import lila.socket._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket
import makeTimeout.short

private[round] final class RoundSocket(
    dependencies: RoundSocket.Dependencies,
    gameId: Game.ID,
    history: History,
    keepMeAlive: () => Unit
) extends SocketTrouper[Member](dependencies.system, dependencies.uidTtl) {

  import dependencies._

  private var hasAi = false
  private var mightBeSimul = true // until proven false
  private var chatIds = RoundSocket.ChatIds(
    priv = Chat.Id(gameId), // until replaced with tourney/simul chat
    pub = Chat.Id(s"$gameId/w")
  )
  private var tournamentId = none[String] // until set, to listen to standings

  private var delayedCrowdNotification = false

  private final class Player(color: Color) {

    // when the player has been seen online for the last time
    private var time: Long = nowMillis
    // wether the player closed the window intentionally
    private var bye: Int = 0
    // connected as a bot
    private var botConnected: Boolean = false

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
      lilaBus.ask[Set[String]]('simulGetHosts)(GetHostIds).map(_ contains u)
    }

    def isGone: Fu[Boolean] = {
      time < (nowMillis - (if (isBye) ragequitTimeout else disconnectTimeout).toMillis) &&
        !botConnected
    } ?? !isHostingSimul

    def setBotConnected(v: Boolean) =
      botConnected = v

    def isBotConnected = botConnected
  }

  private val whitePlayer = new Player(White)
  private val blackPlayer = new Player(Black)

  buscriptions.subAll
  GameRepo game gameId map SetGame.apply foreach this.!

  override def stop(): Unit = {
    buscriptions.unsubAll
    super.stop()
  }

  private object buscriptions {

    private var classifiers = collection.mutable.Set.empty[Symbol]

    private def sub(classifier: Symbol) = {
      lilaBus.subscribe(RoundSocket.this, classifier)
      classifiers += classifier
    }

    def unsubAll = {
      lilaBus.unsubscribe(RoundSocket.this, classifiers.toSeq)
      classifiers.clear
    }

    def subAll = {
      tv
      chat
      tournament
    }

    def tv = members.flatMap { case (_, m) => m.userTv }.toSet foreach { (userId: String) =>
      sub(Symbol(s"userStartGame:$userId"))
    }

    def chat = chatIds.all foreach { chatId =>
      sub(lila.chat.Chat classify chatId)
    }

    def tournament = tournamentId foreach { id =>
      sub(Symbol(s"tour-standing-$id"))
    }
  }

  def receiveSpecific: Trouper.Receive = ({

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

    // from lilaBus 'startGame
    // sets definitive user ids
    // in case one joined after the socket creation
    case StartGame(game) => this ! SetGame(game.some)

    case d: Deploy =>
      onDeploy(d)
      history.enablePersistence

    case BotConnected(color, v) =>
      playerDo(color, _ setBotConnected v)
      notifyCrowd

    case Bye(color) => playerDo(color, _.setBye)

    case IsGone(color, promise) => promise completeWith playerGet(color, _.isGone)

    case IsOnGame(color, promise) => promise success ownerIsHere(color)

    case GetSocketStatus(promise) =>
      playerGet(White, _.isGone) zip playerGet(Black, _.isGone) foreach {
        case (whiteIsGone, blackIsGone) => promise success SocketStatus(
          version = history.getVersion,
          whiteOnGame = ownerIsHere(White),
          whiteIsGone = whiteIsGone,
          blackOnGame = ownerIsHere(Black),
          blackIsGone = blackIsGone
        )
      }

    case Join(uid, user, color, playerId, onTv, version, mobile, promise) => {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, color, playerId, onTv.map(_.userId))
      addMember(uid, member)
      notifyCrowd
      if (playerId.isDefined) playerDo(color, _.ping)
      val reloadTvEvent = onTv ?? {
        case UserTv(_, reload) => reload map {
          case true => SocketTrouper.resyncMessage.some
          case false =>
            buscriptions.tv // reload buscriptions
            none
        }
      }
      val events = version.fold(history.getRecentEvents(5).some) { v =>
        history.getEventsSince(v, lila.mon.round.history(mobile).some) match {
          case History.Types.UpToDate => Nil.some
          case History.Types.Events(e) => e.some
          case _ => None
        }
      }

      val initialMsgs = events.fold(
        SocketTrouper.resyncMsgWithDebug(s"join,$debugString,cv($version)").some
      ) {
          batchMsgsDebug(member, _, s"join,$debugString,cv($version)")
        } map { m => Enumerator(m: JsValue) }

      val fullEnumerator = lila.common.Iteratee.prependFu(
        reloadTvEvent.map(_.toList),
        initialMsgs.fold(enumerator) { _ >>> enumerator }
      )

      promise success Connected(fullEnumerator, member)
    }

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

  }: Trouper.Receive) orElse lila.chat.Socket.out(
    send = (t, d, _) => notifyAll(t, d)
  )

  override def broom = {
    super.broom
    if (members.nonEmpty) keepMeAlive()
    if (!hasAi) Color.all foreach { c =>
      playerGet(c, _.isGone) foreach { _ ?? notifyGone(c, true) }
    }
  }

  override protected def afterQuit(uid: Socket.Uid, member: Member) = notifyCrowd

  def debugString = s"sid:$uniqueId,sv(${history.versionDebugString})"

  def notifyCrowd: Unit = if (isAlive) {
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      system.scheduler.scheduleOnce(1 second)(this ! NotifyCrowd)
    }
  }

  def notify(events: Events): Unit = {
    val vevents = history addEvents events
    members.foreachValue { m =>
      batchMsgs(m, vevents) foreach m.push
    }
  }

  def batchMsgs(member: Member, vevents: List[VersionedEvent]) = vevents match {
    case Nil => None
    case List(one) => one.jsFor(member).some
    case many => makeMessage("b", many map (_ jsFor member)).some
  }

  def batchMsgsDebug(member: Member, vevents: List[VersionedEvent], debug: => String) = {
    if (Env.current.socketDebug()) makeMessageDebug("b", vevents map (_ jsFor member), debug).some
    else batchMsgs(member, vevents)
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

  def ownerIsHere(color: Color) = playerGet(color, _.isBotConnected) ||
    members.values.exists { m =>
      m.owner && m.color == color
    }

  def ownerOf(uid: Socket.Uid): Option[Member] =
    members get uid.value filter (_.owner)

  def foreachWatcher(f: Member => Unit): Unit = members.foreachValue { m =>
    if (m.watcher) f(m)
  }

  private def playerGet[A](color: Color, getter: Player => A): A =
    getter(color.fold(whitePlayer, blackPlayer))

  def playerDo(color: Color, effect: Player => Unit): Unit =
    effect(color.fold(whitePlayer, blackPlayer))
}

object RoundSocket {

  case class ChatIds(priv: Chat.Id, pub: Chat.Id) {
    def all = Seq(priv, pub)
    def update(g: Game) =
      g.tournamentId.map { id => copy(priv = Chat.Id(id)) } orElse
        g.simulId.map { id => copy(priv = Chat.Id(id)) } getOrElse
        this
  }

  private[round] case class Dependencies(
      system: ActorSystem,
      lightUser: LightUser.Getter,
      uidTtl: FiniteDuration,
      disconnectTimeout: FiniteDuration,
      ragequitTimeout: FiniteDuration
  )
}
