package lila.practice

import scala.concurrent.duration.FiniteDuration

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json.JsValue

import lila.socket._

private final class PracticeSocket(
    timeout: FiniteDuration) extends SocketActor[PracticeSocket.Member](timeout) {

  import PracticeSocket._

  def receiveSpecific = {

    case Join(uid, userId) => {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, userId)
      addMember(uid.value, member)
      sender ! Connected(enumerator, member)
    }
  }
}

private object PracticeSocket {

  case class Member(
      channel: JsChannel,
      userId: Option[lila.user.User.ID]) extends SocketMember {

    val troll = false
  }

  case class Join(uid: Socket.Uid, userId: Option[lila.user.User.ID])
  case class Connected(enumerator: JsEnumerator, member: Member)
}
