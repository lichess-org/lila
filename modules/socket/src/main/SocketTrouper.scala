package lila.socket

import scala.concurrent.duration._
import scala.concurrent.Promise
import play.api.libs.iteratee._
import play.api.libs.json.JsValue

import lila.hub.actorApi.HasUserIdP
import lila.hub.Trouper

abstract class SocketTrouper[M <: SocketMember](
    val uidTtl: Duration
) extends SocketBase[M] with Trouper {

  case class AddMember(uid: Socket.Uid, member: M, promise: Promise[Unit])

  override def stop() = {
    super.stop()
    members foreachKey ejectUidString
  }

  protected val receiveTrouper: PartialFunction[Any, Unit] = {

    case HasUserIdP(userId, promise) => promise success hasUserId(userId)

    case AddMember(uid, member, promise) =>
      addMember(uid, member)
      promise.success(())
  }

  val process = receiveSpecific orElse receiveTrouper orElse receiveGeneric

  def addMember(uid: Socket.Uid)(make: JsChannel => M): Fu[(M, JsEnumerator)] = {
    val (enumerator, channel) = Concurrent.broadcast[JsValue]
    val member = make(channel)
    ask[Unit](AddMember(uid, member, _)) inject (member -> enumerator)
  }
}
