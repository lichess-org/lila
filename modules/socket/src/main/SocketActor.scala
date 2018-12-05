package lidraughts.socket

import scala.concurrent.duration._

import akka.actor.{ Deploy => _, _ }

import lidraughts.hub.actorApi.HasUserId

abstract class SocketActor[M <: SocketMember](val uidTtl: Duration) extends SocketBase[M] with Actor {

  val system = context.system

  // this socket is created during application boot
  // and therefore should delay its publication
  // to ensure the listener is ready (sucks, I know)
  val startsOnApplicationBoot: Boolean = false

  override def preStart: Unit = {
    if (startsOnApplicationBoot)
      context.system.scheduler.scheduleOnce(1 second) {
        lidraughtsBus.publish(lidraughts.socket.SocketHub.Open(self), 'socket)
      }
    else lidraughtsBus.publish(lidraughts.socket.SocketHub.Open(self), 'socket)
  }

  override def postStop(): Unit = {
    super.postStop()
    lidraughtsBus.publish(lidraughts.socket.SocketHub.Close(self), 'socket)
    members foreachKey ejectUidString
  }

  val receiveActor: PartialFunction[Any, Unit] = {
    case HasUserId(userId) => sender ! hasUserId(userId)
  }

  def receive = receiveSpecific orElse receiveActor orElse receiveGeneric
}
