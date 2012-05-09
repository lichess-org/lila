package lila
package game

import model._
import socket._
import chess.{ Color, White, Black }

import akka.actor._
import akka.util.duration._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.Play.current
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

    case Ping(uid) ⇒ {
      ping(uid)
      lastPingTime = nowMillis
      ownerOf(uid) foreach { o ⇒
        playerTime(o.color, lastPingTime)
      }
    }

    case Broom ⇒ {
      broom()
      if (lastPingTime < (nowMillis - hubTimeout)) {
        context.parent ! CloseGame(gameId)
      }
      Color.all foreach { c ⇒
        if (playerIsGone(c)) notifyOwner(!c, "gone", JsNull)
      }
    }

    case GetGameVersion(_)           ⇒ sender ! history.version

    case IsConnectedOnGame(_, color) ⇒ sender ! ownerOf(color).isDefined

    case Join(uid, username, version, color, owner) ⇒ {
      val msgs = {
        history since version filter (_.visible(color, owner)) map (_.js)
      } :+ makeEvent("crowd", owner.fold(
        crowdEvent,
        crowdEvent.incWatchers
      ).data)
      val channel = new LilaEnumerator[JsValue](msgs)
      val member = Member(channel, username, PovRef(gameId, color), owner)
      addMember(uid, member)
      notify(crowdEvent)
      sender ! Connected(member)
    }

    case Events(events)        ⇒ applyEvents(events)
    case GameEvents(_, events) ⇒ applyEvents(events)

    case Quit(uid) ⇒ {
      quit(uid)
      notify(crowdEvent)
    }

    case Close ⇒ {
      members.values foreach { _.channel.close() }
      self ! PoisonPill
    }
  }

  def crowdEvent = CrowdEvent(
    white = ownerOf(White).isDefined,
    black = ownerOf(Black).isDefined,
    watchers = members.values count (_.watcher))

  def applyEvents(events: List[Event]) {
    events match {
      case Nil           ⇒
      case single :: Nil ⇒ notify(single)
      case multi         ⇒ notify(multi)
    }
  }

  def notify(e: Event) {
    val vevent = history += e
    members.values filter vevent.visible foreach (_.channel push vevent.js)
  }

  def notify(events: List[Event]) {
    val vevents = events map history.+=
    members.values foreach { member ⇒
      member.channel push JsObject(Seq(
        "t" -> JsString("batch"),
        "d" -> JsArray(vevents filter (_ visible member) map (_.js))
      ))
    }
  }

  def notifyOwner(color: Color, t: String, data: JsValue) {
    ownerOf(color) foreach { m ⇒
      m.channel push makeEvent(t, data)
    }
  }

  def makeEvent(t: String, data: JsValue): JsObject =
    JsObject(Seq("t" -> JsString(t), "d" -> data))

  def ownerOf(color: Color): Option[Member] =
    members.values find { m ⇒ m.owner && m.color == color }

  def ownerOf(uid: String): Option[Member] =
    member(uid) filter (_.owner)

  def playerTime(color: Color): Double = color.fold(
    whiteTime,
    blackTime)

  def playerTime(color: Color, time: Double) {
    color.fold(whiteTime = time, blackTime = time)
  }

  def playerIsGone(color: Color) =
    playerTime(color) < (nowMillis - playerTimeout)
}
