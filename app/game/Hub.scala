package lila
package game

import socket.{ History, LilaEnumerator }
import model._
import chess.Color

import akka.actor._

import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(gameId: String, history: History) extends Actor {

  private var members = Map.empty[String, Member]

  def receive = {

    case GetVersion ⇒ sender ! Version(history.version)

    case Join(uid, version, color, owner, username) ⇒ {
      val channel = new LilaEnumerator[JsValue](history since version)
      val member = Member(channel, color, owner, username)
      members = members + (uid -> member)
      sender ! Connected(member)
    }

    case Events(events) ⇒ events foreach notifyEvent

    case Quit(uid)      ⇒ { members = members - uid }
  }

  private def notifyEvent(e: Event) {
    notifyVersion(e.typ, e.data)
  }

  private def notifyMember(t: String, data: JsValue)(member: Member) {
    val msg = JsObject(Seq("t" -> JsString(t), "d" -> data))
    member.channel push msg
  }

  private def notifyAll(t: String, data: JsValue) {
    val msg = makeMessage(t, data)
    members.values.foreach(_.channel push msg)
  }

  private def notifyVersion(t: String, data: JsValue) {
    val vmsg = history += makeMessage(t, data)
    members.values.foreach(_.channel push vmsg)
  }
  private def notifyVersion(t: String, data: Seq[(String, JsValue)]) {
    notifyVersion(t, JsObject(data))
  }

  private def makeMessage(t: String, data: JsValue) =
    JsObject(Seq("t" -> JsString(t), "d" -> data))
}
