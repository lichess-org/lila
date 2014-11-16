package lila.socket

import scala.concurrent.duration._
import scala.util.Random

import akka.actor.{ Deploy => _, _ }
import play.api.libs.json._
import play.twirl.api.Html

import actorApi._
import lila.hub.actorApi.{ Deploy, GetUids }
import lila.memo.ExpireSetMemo

abstract class SocketActor[M <: SocketMember](uidTtl: Duration) extends Socket with Actor {

  val members = scala.collection.mutable.Map.empty[String, M]
  val aliveUids = new ExpireSetMemo(uidTtl)
  var pong = Socket.initialPong

  val lilaBus = context.system.lilaBus

  lilaBus.publish(lila.socket.SocketHub.Open(self), 'socket)

  override def postStop() {
    lilaBus.publish(lila.socket.SocketHub.Close(self), 'socket)
    members.keys foreach eject
  }

  // to be defined in subclassing actor
  def receiveSpecific: Receive

  // generic message handler
  def receiveGeneric: Receive = {

    case Ping(uid)             => ping(uid)

    case Broom                 => broom

    // when a member quits
    case Quit(uid)             => quit(uid)

    case NbMembers(_, pongMsg) => pong = pongMsg

    case GetUids               => sender ! uids

    case Resync(uid)           => resync(uid)

    case Deploy(event, html)   => notifyAll(makeMessage(event.key, html))
  }

  def receive = receiveSpecific orElse receiveGeneric

  def notifyAll[A: Writes](t: String, data: A) {
    notifyAll(makeMessage(t, data))
  }

  def notifyAll(t: String) {
    notifyAll(makeMessage(t))
  }

  def notifyAll(msg: JsObject) {
    members.values.foreach(_ push msg)
  }

  def notifyMember[A: Writes](t: String, data: A)(member: M) {
    member push makeMessage(t, data)
  }

  def ping(uid: String) {
    setAlive(uid)
    withMember(uid)(_ push pong)
  }

  def broom {
    members.keys foreach { uid =>
      if (!aliveUids.get(uid)) eject(uid)
    }
  }

  def eject(uid: String) {
    withMember(uid) { member =>
      member.end
      quit(uid)
    }
  }

  def quit(uid: String) {
    members get uid foreach { member =>
      members -= uid
      lilaBus.publish(SocketLeave(uid, member), 'socketDoor)
    }
  }

  private val resyncMessage = makeMessage("resync")

  protected def resync(member: M) {
    import scala.concurrent.duration._
    context.system.scheduler.scheduleOnce((Random nextInt 2000).milliseconds) {
      resyncNow(member)
    }
  }

  protected def resync(uid: String) {
    withMember(uid)(resync)
  }

  protected def resyncNow(member: M) {
    member push resyncMessage
  }

  def addMember(uid: String, member: M) {
    eject(uid)
    members += (uid -> member)
    setAlive(uid)
    lilaBus.publish(SocketEnter(uid, member), 'socketDoor)
  }

  def setAlive(uid: String) { aliveUids put uid }

  def uids = members.keys

  def memberByUserId(userId: String): Option[M] =
    members.values find (_.userId == Some(userId))

  def userIds: Iterable[String] = members.values.flatMap(_.userId)

  def showSpectators(users: List[lila.common.LightUser], nbAnons: Int) = nbAnons match {
    case 0 => users.distinct.map(_.titleName)
    case x => users.distinct.map(_.titleName) :+ ("Anonymous (%d)" format x)
  }

  def withMember(uid: String)(f: M => Unit) {
    members get uid foreach f
  }
}
