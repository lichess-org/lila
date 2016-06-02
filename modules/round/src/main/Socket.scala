package lila.round

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.{ Color, White, Black }
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.actorApi.{ StartGame, UserStartGame }
import lila.game.Event
import lila.hub.actorApi.Deploy
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.round.{ IsOnGame, AnalysisAvailable }
import lila.hub.actorApi.tv.{ Select => TvSelect }
import lila.hub.TimeBomb
import lila.socket._
import lila.socket.actorApi.{ Connected => _, _ }
import makeTimeout.short

private[round] final class Socket(
    gameId: String,
    history: History,
    lightUser: String => Option[LightUser],
    uidTimeout: Duration,
    socketTimeout: Duration,
    disconnectTimeout: Duration,
    ragequitTimeout: Duration,
    simulActor: ActorSelection) extends SocketActor[Member](uidTimeout) {

  private var hasAi = false

  private val timeBomb = new TimeBomb(socketTimeout)

  private var delayedCrowdNotification = false

  private final class Player(color: Color) {

    // when the player has been seen online for the last time
    private var time: Long = nowMillis
    // wether the player closed the window intentionally
    private var bye: Int = 0

    var userId = none[String]

    def ping {
      isGone foreach { _ ?? notifyGone(color, false) }
      if (bye > 0) bye = bye - 1
      time = nowMillis
    }
    def setBye {
      bye = 3
    }
    private def isBye = bye > 0

    private def isHostingSimul: Fu[Boolean] = userId ?? { u =>
      simulActor ? lila.hub.actorApi.simul.GetHostIds mapTo manifest[Set[String]] map (_ contains u)
    }

    def isGone =
      if (time < (nowMillis - isBye.fold(ragequitTimeout, disconnectTimeout).toMillis))
        isHostingSimul map (!_)
      else fuccess(false)
  }

  private val whitePlayer = new Player(White)
  private val blackPlayer = new Player(Black)

  override def preStart() {
    super.preStart()
    refreshSubscriptions
    lila.game.GameRepo game gameId map SetGame.apply pipeTo self
  }

  override def postStop() {
    super.postStop()
    lilaBus.unsubscribe(self)
    lilaBus.publish(lila.hub.actorApi.round.SocketEvent.Stop(gameId), 'roundDoor)
  }

  private def refreshSubscriptions {
    lilaBus.unsubscribe(self)
    watchers.flatMap(_.userTv).toList.distinct foreach { userId =>
      lilaBus.subscribe(self, Symbol(s"userStartGame:$userId"))
    }
  }

  def receiveSpecific = {

    case SetGame(Some(game)) =>
      hasAi = game.hasAi
      whitePlayer.userId = game.player(White).userId
      blackPlayer.userId = game.player(Black).userId

    // from lilaBus 'startGame
    // sets definitive user ids
    // in case one joined after the socket creation
    case StartGame(game) => self ! SetGame(game.some)

    case d: Deploy =>
      onDeploy(d)
      history.enablePersistence

    case PingVersion(uid, v) =>
      timeBomb.delay
      ping(uid)
      ownerOf(uid) foreach { o =>
        playerDo(o.color, _.ping)
      }
      withMember(uid) { member =>
        (history getEventsSince v).fold(resyncNow(member))(batch(member, _))
      }

    case Bye(color) => playerDo(color, _.setBye)

    case Broom =>
      broom
      if (timeBomb.boom) self ! PoisonPill
      else if (!hasAi) Color.all foreach { c =>
        playerGet(c, _.isGone) foreach { _ ?? notifyGone(c, true) }
      }

    case GetVersion      => sender ! history.getVersion

    case IsGone(color)   => playerGet(color, _.isGone) pipeTo sender

    case IsOnGame(color) => sender ! ownerOf(color).isDefined

    case GetSocketStatus =>
      playerGet(White, _.isGone) zip playerGet(Black, _.isGone) map {
        case (whiteIsGone, blackIsGone) => SocketStatus(
          version = history.getVersion,
          whiteOnGame = ownerOf(White).isDefined,
          whiteIsGone = whiteIsGone,
          blackOnGame = ownerOf(Black).isDefined,
          blackIsGone = blackIsGone)
      } pipeTo sender

    case Join(uid, user, color, playerId, ip, userTv) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, color, playerId, ip, userTv = userTv)
      addMember(uid, member)
      notifyCrowd
      playerDo(color, _.ping)
      sender ! Connected(enumerator, member)
      if (member.userTv.isDefined) refreshSubscriptions
      if (member.owner) lilaBus.publish(
        lila.hub.actorApi.round.SocketEvent.OwnerJoin(gameId, color, ip),
        'roundDoor)

    case Nil                  =>
    case eventList: EventList => notify(eventList.events)

    case lila.chat.actorApi.ChatLine(chatId, line) => notify(List(line match {
      case l: lila.chat.UserLine   => Event.UserMessage(l, chatId endsWith "/w")
      case l: lila.chat.PlayerLine => Event.PlayerMessage(l)
    }))

    case AnalysisAvailable => notifyAll("analysisAvailable")

    case lila.analyse.actorApi.AnalysisProgress(ratio, analysis) =>
      notifyAll("analysisProgress", Json.obj(
        "ratio" -> lila.common.Maths.truncateAt(ratio, 2),
        "analysis" -> analysis.infos.filterNot(_.isEmpty).map { info =>
          Json.obj(
            "ply" -> info.ply,
            "cp" -> info.score.map(_.centipawns),
            "mate" -> info.mate).noNull
        }
      ))

    case Quit(uid) =>
      members get uid foreach { member =>
        quit(uid)
        notifyCrowd
        if (member.userTv.isDefined) refreshSubscriptions
      }

    case ChangeFeatured(_, msg) => watchers.foreach(_ push msg)

    case TvSelect(msg)          => watchers.foreach(_ push msg)

    case UserStartGame(userId, _) => watchers filter (_ onUserTv userId) foreach {
      _ push makeMessage("resync")
    }

    case round.TournamentStanding(id) => owners.foreach {
      _ push makeMessage("tournamentStanding", id)
    }

    case NotifyCrowd =>
      delayedCrowdNotification = false
      val event = Event.Crowd(
        white = ownerOf(White).isDefined,
        black = ownerOf(Black).isDefined,
        watchers = showSpectators(lightUser)(watchers))
      notifyAll(event.typ, event.data)
  }

  def notifyCrowd {
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      context.system.scheduler.scheduleOnce(1 second, self, NotifyCrowd)
    }
  }

  def notify(events: Events) {
    val vevents = history addEvents events
    members.values foreach { m => batch(m, vevents) }
  }

  def batch(member: Member, vevents: List[VersionedEvent]) {
    vevents match {
      case Nil       =>
      case List(one) => member push one.jsFor(member)
      case many      => member push makeMessage("b", many map (_ jsFor member))
    }
  }

  def notifyOwner[A: Writes](color: Color, t: String, data: A) {
    ownerOf(color) foreach { m =>
      m push makeMessage(t, data)
    }
  }

  def notifyGone(color: Color, gone: Boolean) {
    notifyOwner(!color, "gone", gone)
  }

  def ownerOf(color: Color): Option[Member] =
    members.values find { m => m.owner && m.color == color }

  def ownerOf(uid: String): Option[Member] =
    members get uid filter (_.owner)

  def watchers: Iterable[Member] = members.values.filter(_.watcher)

  def owners: Iterable[Member] = members.values.filter(_.owner)

  private def playerGet[A](color: Color, getter: Player => A): A =
    getter(color.fold(whitePlayer, blackPlayer))

  private def playerDo(color: Color, effect: Player => Unit) {
    effect(color.fold(whitePlayer, blackPlayer))
  }
}
