package lila
package game

import socket.{ History, LilaEnumerator }
import model._
import socket._
import chess.{ Color, White, Black }

import akka.actor._
import akka.event.Logging
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.Play.current
import scalaz.effects._

final class Hub(gameId: String, history: History) extends Actor {

  private var members = Map.empty[String, Member]
  private val log = Logging(context.system, this)

  def receive = {

    case WithMembers(op)    ⇒ op(members.values.pp).unsafePerformIO

    case IfEmpty(op)        ⇒ members.isEmpty.fold(op, io()).unsafePerformIO

    case GetVersion         ⇒ sender ! Version(history.version)

    case GetNbMembers       ⇒ sender ! members.size

    case NbPlayers(nb)      ⇒ notifyAll("nbp", JsNumber(nb))

    case IsConnected(color) ⇒ sender ! member(color).isDefined

    case Join(uid, version, color, owner, username) ⇒ {
      val channel = new LilaEnumerator[JsValue](history since version)
      val member = Member(channel, PovRef(gameId, color), owner, username)
      members = members + (uid -> member)
      sender ! Connected(member)
      notifyCrowd()
    }

    case Events(events) ⇒ events foreach notifyVersion

    case Quit(uid) ⇒ {
      members = members - uid
      notifyCrowd()
    }

    case Close ⇒ {
      members.values foreach { _.channel.close() }
      self ! PoisonPill
    }

    case msg ⇒ log.info("GameHub unknown message: " + msg)
  }

  private def notifyCrowd() {
    notifyVersion("crowd", JsObject(Seq(
      "white" -> JsBoolean(member(White).isDefined),
      "black" -> JsBoolean(member(Black).isDefined),
      "watchers" -> JsNumber(members.values count (_.watcher))
    )))
  }

  private def member(color: Color): Option[Member] =
    members.values find { m ⇒ m.owner && m.color == color }

  private def notifyVersion(e: Event) {
    val vmsg = history += makeMessage(e.typ, e.data)
    val m1 = if (e.owner) members.values filter (_.owner) else members.values
    val m2 = e.only.fold(color ⇒ m1 filter (_.color == color), m1)

    m2 foreach (_.channel push vmsg)
  }
  private def notifyVersion(t: String, d: JsValue) {
    notifyVersion(new Event {
      val typ = t
      val data = d
    })
  }

  private def notifyAll(t: String, data: JsValue) {
    val msg = makeMessage(t, data)
    members.values.foreach(_.channel push msg)
  }

  private def makeMessage(t: String, data: JsValue) =
    JsObject(Seq("t" -> JsString(t), "d" -> data))
}
