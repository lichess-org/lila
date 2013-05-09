package lila.round

import lila.socket._
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import chess.{ Color, White, Black }
import lila.game.Event
import actorApi._
import makeTimeout.short

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.libs.iteratee._

private[round] final class Socket(
    gameId: String,
    history: ActorRef,
    uidTimeout: Duration,
    socketTimeout: Duration,
    playerTimeout: Duration) extends SocketActor[Member](uidTimeout) {

  private var lastPingTime = nowMillis

  // when the players have been seen online for the last time
  private var whiteTime = nowMillis
  private var blackTime = nowMillis

  def receiveSpecific = {

    case PingVersion(uid, v) ⇒ {
      ping(uid)
      lastPingTime = nowMillis
      ownerOf(uid) foreach { o ⇒
        if (playerIsGone(o.color)) notifyGone(o.color, false)
        playerTime(o.color, lastPingTime)
      }
      withMember(uid) { member ⇒
        history ? GetEventsSince(v) foreach {
          case MaybeEvents(events) ⇒ events.fold(resync(member))(batch(member, _))
        }
      }
    }

    case Ack(uid) ⇒ withMember(uid) { _.channel push ackEvent }

    case Broom ⇒ {
      broom()
      if (lastPingTime < (nowMillis - socketTimeout.toMillis)) {
        context.parent ! CloseGame(gameId)
      }
      Color.all foreach { c ⇒
        if (playerIsGone(c)) notifyGone(c, true)
      }
    }

    case GetGameVersion(_)           ⇒ history ? GetVersion pipeTo sender

    case IsConnectedOnGame(_, color) ⇒ sender ! ownerOf(color).isDefined

    case IsGone(_, color)            ⇒ sender ! playerIsGone(color)

    case Join(uid, user, version, color, owner) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, color, owner)
      addMember(uid, member)
      notify(crowdEvent :: Nil)
      if (playerIsGone(color)) notifyGone(color, false)
      playerTime(color, nowMillis)
      sender ! Connected(enumerator, member)
    }

    case Nil           ⇒
    case events: Events        ⇒ notify(events)
    case GameEvents(_, Nil)    ⇒
    case GameEvents(_, events) ⇒ notify(events)

    case msg @ AnalysisAvailable(_) ⇒ {
      notifyAll("analysisAvailable", true)
    }

    case Quit(uid) ⇒ {
      quit(uid)
      notify(crowdEvent :: Nil)
    }

    case Close ⇒ {
      members.values foreach { _.channel.end() }
      self ! PoisonPill
    }
  }

  def crowdEvent = Event.Crowd(
    white = ownerOf(White).isDefined,
    black = ownerOf(Black).isDefined,
    watchers = members.values
      .filter(_.watcher)
      .map(_.userId)
      .toList.partition(_.isDefined) match {
        case (users, anons) ⇒ users.flatten.distinct |> { userList ⇒
          anons.size match {
            case 0 ⇒ userList
            case 1 ⇒ userList :+ "Anonymous"
            case x ⇒ userList :+ ("Anonymous (%d)" format x)
          }
        }
      })

  def notify(events: Events) {
    (events map { event ⇒
      history ? AddEvent(event) mapTo manifest[VersionedEvent]
    }).sequence foreach { vevents ⇒
      members.values foreach { m ⇒ batch(m, vevents) }
    }
  }

  def batch(member: Member, vevents: List[VersionedEvent]) {
    if (vevents.nonEmpty) {
      member.channel push makeEvent("b", List(vevents map (_ jsFor member)))
    }
  }

  def notifyOwner[A : Writes](color: Color, t: String, data: A) {
    ownerOf(color) foreach { m ⇒
      m.channel push makeEvent(t, data)
    }
  }

  def notifyGone(color: Color, gone: Boolean) {
    notifyOwner(!color, "gone", gone)
  }

  def makeEvent[A : Writes](t: String, data: A): JsObject =
    Json.obj("t" -> t, "d" -> data)

  lazy val ackEvent = Json.obj("t" -> "ack")

  def ownerOf(color: Color): Option[Member] =
    members.values find { m ⇒ m.owner && m.color == color }

  def ownerOf(uid: String): Option[Member] =
    members get uid filter (_.owner)

  def playerTime(color: Color): Double = color.fold(
    whiteTime,
    blackTime)

  def playerTime(color: Color, time: Double) {
    color.fold(whiteTime = time, blackTime = time)
  }

  def playerIsGone(color: Color) =
    playerTime(color) < (nowMillis - playerTimeout.toMillis)
}
