package lidraughts.round

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import draughts.{ Color, White, Black, Speed }
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.math.{ log10, sqrt }

import actorApi._
import lidraughts.chat.Chat
import lidraughts.common.LightUser
import lidraughts.game.actorApi.{ SimulNextGame, StartGame, UserStartGame }
import lidraughts.game.{ Game, Event }
import lidraughts.hub.actorApi.Deploy
import lidraughts.hub.actorApi.game.ChangeFeatured
import lidraughts.hub.actorApi.round.{ IsOnGame, TourStanding, SimulStanding }
import lidraughts.hub.actorApi.simul.GetHostIds
import lidraughts.hub.actorApi.tv.{ Select => TvSelect }
import lidraughts.hub.Trouper
import lidraughts.socket._
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Socket
import lidraughts.user.User
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
  private var gameSpeed: Option[Speed] = none
  private var chatIds = RoundSocket.ChatIds(
    priv = Chat.Id(gameId), // until replaced with tourney/simul chat
    pub = Chat.Id(s"$gameId/w")
  )
  private var tournamentId = none[String] // until set, to listen to standings
  private var simulId = none[String]

  private var delayedCrowdNotification = false

  private final class Player(color: Color) {

    // when the player has been seen online for the last time
    private var time: Long = nowMillis
    // wether the player closed the window intentionally
    private var bye: Int = 0
    // connected as a bot
    private var botConnected: Boolean = false

    var userId = none[User.ID]

    private lazy val nbPlaybansForSomeUser: Fu[Int] =
      userId ?? { u =>
        dependencies.playbanApi.bans(List(u)) map { b =>
          b.get(u).getOrElse(0)
        }
      }

    private lazy val sitCounterForSomeUser: Fu[Int] =
      userId ?? { u =>
        dependencies.playbanApi.getSitAndDcCounterById(u)
      }

    // do NOT evaluate nbPlaybansForSomeUser or sitCounterForSomeUser when userId is none
    // doing so will set it to 0 while userId might just not be known yet
    def nbPlaybans: Fu[Int] =
      userId.isDefined ?? nbPlaybansForSomeUser
    def sitCounter: Fu[Int] =
      userId.isDefined ?? sitCounterForSomeUser

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
      lidraughtsBus.ask[Set[User.ID]]('simulGetHosts)(GetHostIds).map(_ contains u)
    }

    def weigh(orig: Long, startAtPlaybans: Int, startAtSit: Int) =
      nbPlaybans zip sitCounter map {
        case (np, sc) =>
          if (np < startAtPlaybans || sc > startAtSit) {
            orig
          } else {
            orig * ((1d - 0.8 * sqrt(log10(np - sc))) atLeast 0.02) // sc is negative for abusers
          }
      }
    def weightedRagequitTimeout = weigh(ragequitTimeout.toMillis, 4, -4)
    def weightedDisconnectTimeout = weigh(gameDisconnectTimeout(gameSpeed).toMillis, 4, -4)

    def isGone: Fu[Boolean] = {
      weightedRagequitTimeout zip weightedDisconnectTimeout map {
        case (rq, dc) =>
          time < (nowMillis - (if (isBye) rq else dc)) &&
            !botConnected
      }
    } ?? !isHostingSimul

    def setBotConnected(v: Boolean) =
      botConnected = v

    def isBotConnected = botConnected
  }

  private val whitePlayer = new Player(White)
  private val blackPlayer = new Player(Black)

  buscriptions.subAll
  getGame(gameId) map SetGame.apply foreach this.!

  override def stop(): Unit = {
    buscriptions.unsubAll
    super.stop()
  }

  private object buscriptions {

    private var classifiers = collection.mutable.Set.empty[Symbol]

    private def sub(classifier: Symbol) = {
      lidraughtsBus.subscribe(RoundSocket.this, classifier)
      classifiers += classifier
    }

    def unsubAll = {
      lidraughtsBus.unsubscribe(RoundSocket.this, classifiers.toSeq)
      classifiers.clear
    }

    def subAll = {
      tv
      chat
      tournament
      simul
    }

    def tv = members.flatMap { case (_, m) => m.userTv }.toSet foreach { (userId: User.ID) =>
      sub(Symbol(s"userStartGame:$userId"))
      sub(Symbol(s"simulNextGame:$userId"))
    }

    def chat = chatIds.all foreach { chatId =>
      sub(lidraughts.chat.Chat classify chatId)
    }

    def tournament = tournamentId foreach { id =>
      sub(Symbol(s"tour-standing-$id"))
    }

    def simul = simulId foreach { id =>
      sub(Symbol(s"simul-standing-$id"))
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
      game.simulId foreach { simId =>
        simulId = simId.some
        buscriptions.simul
      }
      gameSpeed = game.speed.some

    // from lidraughtsBus 'startGame
    // sets definitive user ids
    // in case one joined after the socket creation
    case StartGame(game) => this ! SetGame(game.some)

    case d: Deploy =>
      onDeploy(d)
      history.persistNow()

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
        history.getEventsSince(v, lidraughts.mon.round.history(mobile).some) match {
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

      val fullEnumerator = lidraughts.common.Iteratee.prependFu(
        reloadTvEvent.map(_.toList),
        initialMsgs.fold(enumerator) { _ >>> enumerator }
      )

      promise success Connected(fullEnumerator, member)
    }

    case eventList: EventList => notify(eventList.events)

    case lidraughts.chat.actorApi.ChatLine(chatId, line) => notify(List(line match {
      case l: lidraughts.chat.UserLine => Event.UserMessage(l, chatId == chatIds.pub)
      case l: lidraughts.chat.PlayerLine => Event.PlayerMessage(l)
    }))

    case a: lidraughts.analyse.actorApi.AnalysisProgress =>
      import lidraughts.analyse.{ JsonView => analysisJson }
      notifyAll("analysisProgress", Json.obj(
        "analysis" -> analysisJson.bothPlayers(a.game, a.analysis),
        "tree" -> TreeBuilder(
          id = a.analysis.id,
          pdnmoves = a.game.pdnMoves,
          variant = a.variant,
          analysis = a.analysis.some,
          initialFen = a.initialFen,
          withFlags = JsonView.WithFlags(),
          clocks = none,
          iteratedCapts = a.game.imported,
          mergeCapts = true
        )
      ))

    case ChangeFeatured(_, msg) => foreachWatcher(_ push msg)

    case TvSelect(msg) => foreachWatcher(_ push msg)

    case UserStartGame(userId, game) => foreachWatcher { m =>
      if (m.onUserTv(userId) && !m.userId.exists(game.userIds.contains))
        m push makeMessage("resync")
    }

    case SimulNextGame(hostId, game) => foreachWatcher { m =>
      if (m.onUserTv(hostId) && m.userId.fold(true)(id => !game.userIds.contains(id)))
        m push makeMessage("simultv", game.id)
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
    case SimulStanding(json) => notifyAll("simulStanding", json)

  }: Trouper.Receive) orElse lidraughts.chat.Socket.out(
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
      ragequitTimeout: FiniteDuration,
      playbanApi: lidraughts.playban.PlaybanApi,
      getGame: Game.ID => Fu[Option[Game]]
  ) {

    def gameDisconnectTimeout(speed: Option[Speed]): FiniteDuration =
      disconnectTimeout * speed.fold(1) {
        case Speed.Classical => 3
        case Speed.Rapid => 2
        case _ => 1
      }
  }
}
