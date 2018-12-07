package lila.analyse

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Promise

import lila.hub.Trouper
import lila.socket._

private final class AnalyseSocket(
    val system: akka.actor.ActorSystem,
    uidTtl: FiniteDuration
) extends SocketTrouper[AnalyseSocket.Member](uidTtl) {

  import AnalyseSocket._

  def receiveSpecific: Trouper.Receive = {
    case AddMember(uid, member, promise) =>
      addMember(uid, member)
      promise.success(())
  }
}

private object AnalyseSocket {

  case class Member(
      channel: JsChannel,
      userId: Option[lila.user.User.ID]
  ) extends SocketMember {
    val troll = false
  }

  case class AddMember(uid: Socket.Uid, member: Member, promise: Promise[Unit])
}
