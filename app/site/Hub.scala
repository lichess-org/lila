package lila
package site

import socket._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub extends Actor {

  private var members = Map.empty[String, Member]

  def receive = {

    case GetUsernames  ⇒ sender ! usernames

    case Join(uid, username) ⇒ {
      val channel = new LilaEnumerator[JsValue](Nil)
      members = members + (uid -> Member(channel, username))
      sender ! Connected(channel)
    }

    case NbMembers     ⇒ notifyAll("nbp", JsNumber(members.size))

    case Quit(uid)     ⇒ { members = members - uid }
  }

  private def notifyMember(t: String, data: JsValue)(member: Member) {
    val msg = JsObject(Seq("t" -> JsString(t), "d" -> data))
    member.channel push msg
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
