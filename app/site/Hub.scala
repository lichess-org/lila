package lila
package site

import socket._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(timeout: Int) extends HubActor[Member](timeout) {

  def receiveSpecific = {

    case Join(uid, username) ⇒ {
      val channel = new LilaEnumerator[JsValue](Nil)
      addMember(uid, Member(channel, username))
      sender ! Connected(channel)
    }
  }
}
