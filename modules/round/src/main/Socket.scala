package lila.round

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.{ Color, White, Black }
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.common.LightUser
import lila.game.actorApi.UserStartGame
import lila.game.Event
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.TimeBomb
import lila.round.actorApi.Bye
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
    ragequitTimeout: Duration) extends SocketActor[Member](uidTimeout) {

  private var hasAi = false

  private val timeBomb = new TimeBomb(socketTimeout)

  private final class Player(color: Color) {

    // when the player has been seen online for the last time
    private var time: Double = nowMillis
    // wether the player closed the window intentionally
    private var bye: Int = 0

    def ping {
      if (isGone) notifyGone(color, false)
      if (bye > 0) bye = bye - 1
      time = nowMillis
    }
    def setBye {
      bye = 3
    }
    private def isBye = bye > 0

    def isGone = time < (nowMillis - isBye.fold(ragequitTimeout, disconnectTimeout).toMillis)
  }

  private val whitePlayer = new Player(White)
  private val blackPlayer = new Player(Black)

  override def preStart() {
    refreshSubscriptions
    lila.game.GameRepo game gameId map SetGame.apply pipeTo self
  }

  override def postStop() {
    super.postStop()
    lilaBus.unsubscribe(self)
  }

  private def refreshSubscriptions {
    lilaBus.unsubscribe(self)
    watchers.flatMap(_.userTv).toList.distinct foreach { userId =>
      lilaBus.subscribe(self, Symbol(s"userStartGame:$userId"))
    }
  }

  def receiveSpecific = {

    case SetGame(Some(game)) => hasAi = game.hasAi

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

    case Ack(uid)   => withMember(uid) { _ push ackEvent }

    case Broom =>
      broom
      if (timeBomb.boom) self ! PoisonPill
      else if (!hasAi) Color.all foreach { c =>
        if (playerGet(c, _.isGone)) notifyGone(c, true)
      }

    case GetVersion    => sender ! history.getVersion

    case IsGone(color) => sender ! playerGet(color, _.isGone)

    case GetSocketStatus => sender ! SocketStatus(
      version = history.getVersion,
      whiteOnGame = ownerOf(White).isDefined,
      whiteIsGone = playerGet(White, _.isGone),
      blackOnGame = ownerOf(Black).isDefined,
      blackIsGone = playerGet(Black, _.isGone))

    case Join(uid, user, version, color, playerId, ip, userTv) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, color, playerId, ip, userTv = userTv)
      addMember(uid, member)
      notifyCrowd
      playerDo(color, _.ping)
      sender ! Connected(enumerator, member)
      if (member.userTv.isDefined) refreshSubscriptions

    case Nil                  =>
    case eventList: EventList => notify(eventList.events)

    case lila.chat.actorApi.ChatLine(chatId, line) => notify(List(line match {
      case l: lila.chat.UserLine   => Event.UserMessage(l, chatId endsWith "/w")
      case l: lila.chat.PlayerLine => Event.PlayerMessage(l)
    }))

    case AnalysisAvailable                           => notifyAll("analysisAvailable")

    case lila.hub.actorApi.setup.DeclineChallenge(_) => notifyAll("declined")

    case Quit(uid) =>
      members get uid foreach { member =>
        quit(uid)
        notifyCrowd
        if (member.userTv.isDefined) refreshSubscriptions
      }

    case ChangeFeatured(_, msg) => watchers.foreach(_ push msg)

    case UserStartGame(userId, game) => watchers filter (_ onUserTv userId) foreach {
      _ push makeMessage("resync")
    }
  }

  def notifyCrowd {
    val (anons, users) = watchers.map(_.userId flatMap lightUser).foldLeft(0 -> List[LightUser]()) {
      case ((anons, users), Some(user)) => anons -> (user :: users)
      case ((anons, users), None)       => (anons + 1) -> users
    }
    notify(Event.Crowd(
      white = ownerOf(White).isDefined,
      black = ownerOf(Black).isDefined,
      watchers = showSpectators(users, anons)
    ) :: Nil)
  }

  def notify(events: Events) {
    val vevents = history addEvents events
    members.values foreach { m => batch(m, vevents) }
  }

  def batch(member: Member, vevents: List[VersionedEvent]) {
    if (vevents.nonEmpty) {
      member push makeMessage("b", vevents map (_ jsFor member))
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

  private val ackEvent = Json.obj("t" -> "ack")

  def ownerOf(color: Color): Option[Member] =
    members.values find { m => m.owner && m.color == color }

  def ownerOf(uid: String): Option[Member] =
    members get uid filter (_.owner)

  def watchers: Iterable[Member] = members.values.filter(_.watcher)

  private def playerGet[A](color: Color, getter: Player => A): A =
    getter(color.fold(whitePlayer, blackPlayer))

  private def playerDo(color: Color, effect: Player => Unit) {
    effect(color.fold(whitePlayer, blackPlayer))
  }
}
