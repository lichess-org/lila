package lila.analyse

import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.hub.Trouper
import lila.socket._

private final class AnalyseSocket(
    val system: akka.actor.ActorSystem,
    uidTtl: FiniteDuration
) extends SocketTrouper[AnalyseSocket.Member](uidTtl) with LoneSocket {

  def monitoringName = "analyse"
  def broomFrequency = 4027 millis

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
