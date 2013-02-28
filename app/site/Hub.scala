package lila.app
package site

import socket._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(timeout: Int) extends HubActor[Member](timeout) {

  def receiveSpecific = {

    case Join(uid, username, tags) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      addMember(uid, Member(channel, username, tags))
      sender ! Connected(enumerator, channel)
    }

    case SendToFlag(flag, message) ⇒
      members.values filter (_ hasFlag flag) foreach {
        _.channel push message
      }
  }
}
