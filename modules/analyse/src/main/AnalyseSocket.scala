package lila.analyse

import scala.concurrent.duration.FiniteDuration

import play.api.libs.iteratee._
import play.api.libs.json.JsValue
import scala.concurrent.Promise

import lila.hub.Trouper
import lila.socket._

private final class AnalyseSocket(
    val system: akka.actor.ActorSystem,
    uidTtl: FiniteDuration
) extends SocketTrouper[AnalyseSocket.Member](uidTtl) {

  import AnalyseSocket._

  def receiveSpecific: Trouper.Receive = {

    case JoinP(uid, userId, promise) => {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, userId)
      addMember(uid, member)
      promise success Connected(enumerator, member)
    }
  }
}

private object AnalyseSocket {

  case class Member(
      channel: JsChannel,
      userId: Option[lila.user.User.ID]
  ) extends SocketMember {
    val troll = false
  }

  case class JoinP(uid: Socket.Uid, userId: Option[lila.user.User.ID], promise: Promise[Connected])
  case class Connected(enumerator: JsEnumerator, member: Member)
}
