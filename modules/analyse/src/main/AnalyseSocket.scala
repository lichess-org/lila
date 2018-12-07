package lidraughts.analyse

import scala.concurrent.duration.FiniteDuration

import play.api.libs.iteratee._
import play.api.libs.json.JsValue
import scala.concurrent.Promise

import lidraughts.hub.Trouper
import lidraughts.socket._

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
      userId: Option[lidraughts.user.User.ID]
  ) extends SocketMember {
    val troll = false
  }

  case class JoinP(uid: Socket.Uid, userId: Option[lidraughts.user.User.ID], promise: Promise[Connected])
  case class Connected(enumerator: JsEnumerator, member: Member)
}
