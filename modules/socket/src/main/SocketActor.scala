package lila.socket

import scala.concurrent.duration._

import akka.actor.{ Deploy => _, _ }

import lila.hub.actorApi.HasUserId

abstract class SocketActor[M <: SocketMember](val uidTtl: Duration) extends SocketBase[M] with Actor {

  val system = context.system

  // this socket is created during application boot
  // and therefore should delay its publication
  // to ensure the listener is ready (sucks, I know)
  val startsOnApplicationBoot: Boolean = false

  override def preStart: Unit = {
    if (startsOnApplicationBoot)
      context.system.scheduler.scheduleOnce(1 second) {
        lilaBus.publish(lila.socket.SocketHub.Open(self), 'socket)
      }
    else lilaBus.publish(lila.socket.SocketHub.Open(self), 'socket)
  }

  override def postStop(): Unit = {
    super.postStop()
    lilaBus.publish(lila.socket.SocketHub.Close(self), 'socket)
    members foreachKey ejectUidString
  }

  val receiveActor: PartialFunction[Any, Unit] = {
    case HasUserId(userId) => sender ! hasUserId(userId)
  }

  def receive = receiveSpecific orElse receiveActor orElse receiveGeneric
}
