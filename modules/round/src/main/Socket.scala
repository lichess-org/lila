package lila.round

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import chess.{ Color, White, Black }
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.game.actorApi.ChangeFeaturedGame
import lila.game.Event
import lila.hub.TimeBomb
import lila.round.actorApi.Bye
import lila.socket._
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import makeTimeout.short

private[round] final class Socket(
    gameId: String,
    history: History,
    getUsername: String ⇒ Fu[Option[String]],
    uidTimeout: Duration,
    socketTimeout: Duration,
    disconnectTimeout: Duration,
    ragequitTimeout: Duration) extends SocketActor[Member](uidTimeout) {

  context.system.lilaBus.subscribe(self, 'chatOut)

  private val timeBomb = new TimeBomb(socketTimeout)

  private final class Player(color: Color) {

    // when the player has been seen online for the last time
    private var time: Double = nowMillis
    // wheter the player closed the window (JS unload event)
    private var bye: Boolean = false

    def ping {
      if (isGone) notifyGone(color, false)
      bye = false
      time = nowMillis
    }
    def setBye {
      bye = true
    }
    def isGone = bye && time < (nowMillis - bye.fold(ragequitTimeout, disconnectTimeout).toMillis)
  }

  private val whitePlayer = new Player(White)
  private val blackPlayer = new Player(Black)

  def receiveSpecific = {

    case PingVersion(uid, v) ⇒ {
      timeBomb.delay
      ping(uid)
      ownerOf(uid) foreach { o ⇒
        playerDo(o.color, _.ping)
      }
      withMember(uid) { member ⇒
        (history getEventsSince v).fold(resyncNow(member))(batch(member, _))
      }
    }

    case Bye(color) ⇒ playerDo(color, _.setBye)

    case Ack(uid)   ⇒ withMember(uid) { _.channel push ackEvent }

    case Broom ⇒ {
      broom
      if (timeBomb.boom) self ! PoisonPill
      else Color.all foreach { c ⇒
        if (playerGet(c, _.isGone)) notifyGone(c, true)
      }
    }

    case GetVersion    ⇒ sender ! history.getVersion

    case IsGone(color) ⇒ sender ! playerGet(color, _.isGone)

    case Join(uid, user, version, color, playerId, ip) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, color, playerId, ip)
      addMember(uid, member)
      notifyCrowd
      playerDo(color, _.ping)
      sender ! Connected(enumerator, member)
    }

    case Nil            ⇒
    case events: Events ⇒ notify(events)

    case lila.chat.actorApi.ChatLine(chatId, line) ⇒ notify(List(line match {
      case l: lila.chat.UserLine   ⇒ Event.UserMessage(l, chatId endsWith "/w")
      case l: lila.chat.PlayerLine ⇒ Event.PlayerMessage(l)
    }))

    case AnalysisAvailable                           ⇒ notifyAll("analysisAvailable", true)

    case lila.hub.actorApi.setup.DeclineChallenge(_) ⇒ notifyAll("declined", JsNull)

    case ChangeFeaturedGame(game) ⇒ watchers.nonEmpty ! {
      val msg = makeMessage("featured_id", game.id)
      watchers foreach { _.channel push msg }
    }

    case Quit(uid) ⇒ {
      quit(uid)
      notifyCrowd
    }
  }

  def notifyCrowd {
    watchers.map(_.userId).toList.partition(_.isDefined) match {
      case (users, anons) ⇒
        (users.flatten.distinct map getUsername).sequenceFu map { userList ⇒
          notify(Event.Crowd(
            white = ownerOf(White).isDefined,
            black = ownerOf(Black).isDefined,
            watchers = showSpectators(userList.flatten, anons.size)
          ) :: Nil)
        } logFailure ("[round] notify crowd")
    }
  }

  def notify(events: Events) {
    val vevents = history addEvents events
    members.values foreach { m ⇒ batch(m, vevents) }
  }

  def batch(member: Member, vevents: List[VersionedEvent]) {
    if (vevents.nonEmpty) {
      member.channel push makeMessage("b", vevents map (_ jsFor member))
    }
  }

  def notifyOwner[A: Writes](color: Color, t: String, data: A) {
    ownerOf(color) foreach { m ⇒
      m.channel push makeMessage(t, data)
    }
  }

  def notifyGone(color: Color, gone: Boolean) {
    notifyOwner(!color, "gone", gone)
  }

  private val ackEvent = Json.obj("t" -> "ack")

  def ownerOf(color: Color): Option[Member] =
    members.values find { m ⇒ m.owner && m.color == color }

  def ownerOf(uid: String): Option[Member] =
    members get uid filter (_.owner)

  def watchers: List[Member] = members.values.filter(_.watcher).toList

  private def playerGet[A](color: Color, getter: Player ⇒ A): A =
    getter(color.fold(whitePlayer, blackPlayer))

  private def playerDo(color: Color, effect: Player ⇒ Unit) {
    effect(color.fold(whitePlayer, blackPlayer))
  }
}
