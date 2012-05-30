package lila
package monitor

import socket._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(timeout: Int) extends HubActor[Member](timeout) {

  def receiveSpecific = {

    case Join(uid) ⇒ {
      val channel = new LilaEnumerator[JsValue](Nil)
      addMember(uid, Member(channel))
      sender ! Connected(channel)
    }

    case MonitorData(data) ⇒ notifyAll("monitor", JsString(data mkString ";"))
  }
}
