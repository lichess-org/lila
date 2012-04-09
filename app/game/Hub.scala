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

    case Events(events) ⇒ events.pp foreach notifyEvent

    case Quit(uid)      ⇒ { members = members - uid }
  }

  private def notifyEvent(e: Event) {
    val vmsg = history += makeMessage(e.typ, e.data)
    val m1 = if (e.owner) members.values filter (_.owner) else members.values
    val m2 = e.only.fold(color ⇒ m1 filter (_.color == color), m1)

    m2 foreach (_.channel push vmsg)
  }

  private def makeMessage(t: String, data: JsValue) =
    JsObject(Seq("t" -> JsString(t), "d" -> data))
}
