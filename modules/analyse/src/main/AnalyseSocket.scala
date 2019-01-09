package lila.analyse

import play.api.libs.iteratee._
import play.api.libs.json.JsValue
import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.hub.Trouper
import lila.socket._

private final class AnalyseSocket(
    system: akka.actor.ActorSystem,
    uidTtl: FiniteDuration
) extends SocketTrouper[AnalyseSocket.Member](system, uidTtl) with LoneSocket {

  def monitoringName = "analyse"
  def broomFrequency = 4027 millis

  import AnalyseSocket._

  def receiveSpecific = {

    case Join(uid, userId, promise) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, userId)
      addMember(uid, member)
      promise success Connected(enumerator, member)
  }
}

private object AnalyseSocket {

  case class Member(
      channel: JsChannel,
      userId: Option[lila.user.User.ID]
  ) extends SocketMember

  private[analyse] case class Join(uid: Socket.Uid, userId: Option[String], promise: Promise[Connected])
  private[analyse] case class Connected(enumerator: JsEnumerator, member: Member)
}
