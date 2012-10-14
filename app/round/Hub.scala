package lila
package round

import socket._
import chess.{ Color, White, Black }

import akka.actor._
import akka.util.duration._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import scalaz.effects._

final class Hub(
    gameId: String,
    history: History,
    uidTimeout: Int,
    hubTimeout: Int,
    playerTimeout: Int) extends HubActor[Member](uidTimeout) {

  var lastPingTime = nowMillis

  // when the players have been seen online for the last time
  var whiteTime = nowMillis
  var blackTime = nowMillis

  def receiveSpecific = {

    case PingVersion(uid, v) ⇒ {
      ping(uid)
      lastPingTime = nowMillis
      ownerOf(uid) foreach { o ⇒
        if (playerIsGone(o.color)) notifyGone(o.color, false)
        playerTime(o.color, lastPingTime)
      }
      withMember(uid) { m ⇒
        history.since(v).fold(batch(m, _), resync(m))
      }
    }

    case Ack(uid) ⇒ withMember(uid) { _.channel push ackEvent }

    case Broom ⇒ {
      broom()
      if (lastPingTime < (nowMillis - hubTimeout)) {
        context.parent ! CloseGame(gameId)
      }
      Color.all foreach { c ⇒
        if (playerIsGone(c)) notifyGone(c, true)
      }
    }

    case GetGameVersion(_)           ⇒ sender ! history.version

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

    case Events(Nil)           ⇒
    case Events(events)        ⇒ notify(events)
    case GameEvents(_, Nil)    ⇒
    case GameEvents(_, events) ⇒ notify(events)

    case msg @ AnalysisAvailable(_) ⇒ {
      notifyAll("analysisAvailable", JsNull)
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
      .map(_.username)
      .toList.partition(_.isDefined) match {
        case (users, anons) ⇒ users.flatten.distinct |> { userList ⇒
          anons.size match {
            case 0 ⇒ userList
            case 1 ⇒ userList :+ "Anonymous"
            case x ⇒ userList :+ ("Anonymous (%d)" format x)
          }
        }
      })

  def notify(events: List[Event]) {
    val vevents = events map history.+=
    members.values foreach { m ⇒ batch(m, vevents) }
  }

  def batch(member: Member, vevents: List[VersionedEvent]) {
    if (vevents.nonEmpty) {
      member.channel push makeEvent("b", JsArray(vevents map (_ jsFor member)))
    }
  }

  def notifyOwner(color: Color, t: String, data: JsValue) {
    ownerOf(color) foreach { m ⇒
      m.channel push makeEvent(t, data)
    }
  }

  def notifyGone(color: Color, gone: Boolean) {
    notifyOwner(!color, "gone", JsBoolean(gone))
  }

  def makeEvent(t: String, data: JsValue): JsObject =
    JsObject(Seq("t" -> JsString(t), "d" -> data))

  lazy val ackEvent = makeEvent("ack", JsNull)

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

  def playerIsGone(color: Color) = playerTime(color) < (nowMillis - playerTimeout)
}
