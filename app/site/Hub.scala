package lila
package site

import socket._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(timeout: Int) extends HubActor[Member](timeout) {

  def receiveSpecific = {

    case WithUsernames(op) ⇒ op(usernames).unsafePerformIO

    case Join(uid, username) ⇒ {
      val channel = new LilaEnumerator[JsValue](Nil)
      addMember(uid, Member(channel, username))
      sender ! Connected(channel)
    }

    case NbMembers    ⇒ notifyAll("n", JsNumber(members.size))
  }

  private def notifyAll(t: String, data: JsValue) {
    val msg = makeMessage(t, data)
    members.values.foreach(_.channel push msg)
  }

  private def usernames: Iterable[String] = members.values collect {
    case Member(_, Some(username)) ⇒ username
  }

  private def makeMessage(t: String, data: JsValue) =
    JsObject(Seq("t" -> JsString(t), "d" -> data))
}
