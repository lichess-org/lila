package lila.socket

import scala.concurrent.duration._
import scala.concurrent.Promise
import play.api.libs.iteratee._
import play.api.libs.json.JsValue
import ornicar.scalalib.Random.approximatly

import lila.hub.actorApi.HasUserIdP
import lila.hub.Trouper

abstract class SocketTrouper[M <: SocketMember](
    val uidTtl: Duration
) extends SocketBase[M] with Trouper {

  import SocketTrouper._

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

    case GetNbMembers(promise) => promise success members.size
  }

  val process = receiveSpecific orElse receiveTrouper orElse receiveGeneric

  def addMember(uid: Socket.Uid)(make: JsChannel => M): Fu[(M, JsEnumerator)] = {
    val (enumerator, channel) = Concurrent.broadcast[JsValue]
    val member = make(channel)
    ask[Unit](AddMember(uid, member, _)) inject (member -> enumerator)
  }
}

object SocketTrouper {
  case class GetNbMembers(promise: Promise[Int])
}

// Not managed by a TrouperMap
trait LoneSocket { self: SocketTrouper[_] =>

  def monitoringName: String
  def broomFrequency: FiniteDuration

  system.scheduler.schedule(approximatly(0.9f)(10.seconds.toMillis).millis.pp, broomFrequency) {
    this ! lila.socket.actorApi.Broom
    lila.mon.socket.queueSize(monitoringName)(estimateQueueSize)
  }
  system.lilaBus.subscribe(this, 'deploy)
}
