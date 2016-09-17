package lila.site

import scala.concurrent.duration.Duration

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.socket._
import lila.socket.actorApi.SendToFlag

private[site] final class Socket(timeout: Duration) extends SocketActor[Member](timeout) {

  override val startsOnApplicationBoot = true

  def receiveSpecific = {

    case Join(uid, sameOrigin, username, tags) => {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, sameOrigin, username, tags)
      addMember(uid, member)
      sender ! Connected(enumerator, member)
    }

    case SendToFlag(flag, message) =>
      members.values filter (_ hasFlag flag) foreach {
        _.channel push message
      }
  }
}
