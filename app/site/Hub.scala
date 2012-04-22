package lila
package site

import socket._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub extends Actor {

  private var members = Map.empty[String, Member]
  private val pinger = new Pinger

  def receive = {

    case Ping(uid) ⇒ {
      members get uid foreach { _.channel push Util.pong }
    }

    case Cleanup ⇒ members.keys filterNot pingers.get foreach { uid ⇒
      self ! Quit(uid)
    }

    case WithUsernames(op) ⇒ op(usernames).unsafePerformIO

    case Join(uid, username) ⇒ {
      val channel = new LilaEnumerator[JsValue](Nil)
      members = members + (uid -> Member(channel, username))
      sender ! Connected(channel)
      pingers putUnsafe uid
    }

    case NbMembers    ⇒ notifyAll("n", JsNumber(members.size))

    case GetNbMembers ⇒ sender ! members.size

    case Quit(uid) ⇒ {
      members = members - uid
    }
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
