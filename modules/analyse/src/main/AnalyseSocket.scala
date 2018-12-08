package lidraughts.analyse

import scala.concurrent.duration._
import scala.concurrent.Promise

import lidraughts.hub.Trouper
import lidraughts.socket._

private final class AnalyseSocket(
    val system: akka.actor.ActorSystem,
    uidTtl: FiniteDuration
) extends SocketTrouper[AnalyseSocket.Member](uidTtl) {

  system.scheduler.schedule(10 seconds, 4027 millis) {
    lidraughts.mon.socket.queueSize("analyse")(estimateQueueSize)
    this ! lidraughts.socket.actorApi.Broom
  }
  system.lidraughtsBus.subscribe(this, 'deploy)

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
