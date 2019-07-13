package lila.analyse

import play.api.libs.iteratee._
import play.api.libs.json.JsValue
import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.hub.Trouper
import lila.socket._

private final class AnalyseSocket(
    system: akka.actor.ActorSystem,
    sriTtl: FiniteDuration
) extends SocketTrouper[AnalyseSocket.Member](system, sriTtl) with LoneSocket {

  def monitoringName = "analyse"
  def broomFrequency = 4027 millis

  import AnalyseSocket._

  def receiveSpecific = {

    case Join(sri, userId, promise) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, userId)
      addMember(sri, member)
      promise success Connected(enumerator, member)
  }
}

private object AnalyseSocket {

  case class Member(
      channel: JsChannel,
      userId: Option[lila.user.User.ID]
  ) extends SocketMember

  private[analyse] case class Join(sri: Socket.Sri, userId: Option[String], promise: Promise[Connected])
  private[analyse] case class Connected(enumerator: JsEnumerator, member: Member)
}
