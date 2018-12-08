package lila.analyse

import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.hub.Trouper
import lila.socket._

private final class AnalyseSocket(
    val system: akka.actor.ActorSystem,
    uidTtl: FiniteDuration
) extends SocketTrouper[AnalyseSocket.Member](uidTtl) {

  system.scheduler.schedule(10 seconds, 4027 millis) {
    lila.mon.socket.queueSize("analyse")(estimateQueueSize)
    this ! lila.socket.actorApi.Broom
  }
  system.lilaBus.subscribe(this, 'deploy)

  def receiveSpecific = PartialFunction.empty
}

private object AnalyseSocket {

  case class Member(
      channel: JsChannel,
      userId: Option[lila.user.User.ID]
  ) extends SocketMember {
    val troll = false
  }
}
