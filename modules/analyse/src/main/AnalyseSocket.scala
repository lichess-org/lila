package lidraughts.analyse

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Promise

import lidraughts.hub.Trouper
import lidraughts.socket._

private final class AnalyseSocket(
    val system: akka.actor.ActorSystem,
    uidTtl: FiniteDuration
) extends SocketTrouper[AnalyseSocket.Member](uidTtl) {

  import AnalyseSocket._

  def receiveSpecific = PartialFunction.empty
}

private object AnalyseSocket {

  case class Member(
      channel: JsChannel,
      userId: Option[lidraughts.user.User.ID]
  ) extends SocketMember {
    val troll = false
  }
}
