package lila
package monitor

import socket._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(timeout: Int) extends HubActor[Member](timeout) {

  def receiveSpecific = {

    case Join(uid) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      addMember(uid, Member(channel))
      sender ! Connected(enumerator, channel)
    }

    case MonitorData(data) ⇒ notifyAll("monitor", JsString(data mkString ";"))
  }
}
