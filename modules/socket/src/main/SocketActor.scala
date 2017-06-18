package lila.socket

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random

import akka.actor.{ Deploy => _, _ }
import play.api.libs.json._

import actorApi._
import lila.common.LightUser
import lila.hub.actorApi.{ Deploy, HasUserId }
import lila.memo.ExpireSetMemo

abstract class SocketActor[M <: SocketMember](uidTtl: Duration) extends Socket with Actor {

  val members = scala.collection.mutable.AnyRefMap.empty[String, M]
  val aliveUids = new ExpireSetMemo(uidTtl)
  var pong = initialPong

  val lilaBus = context.system.lilaBus

  // this socket is created during application boot
  // and therefore should delay its publication
  // to ensure the listener is ready (sucks, I know)
  val startsOnApplicationBoot: Boolean = false

  override def preStart {
    if (startsOnApplicationBoot)
      context.system.scheduler.scheduleOnce(1 second) {
        lilaBus.publish(lila.socket.SocketHub.Open(self), 'socket)
      }
    else lilaBus.publish(lila.socket.SocketHub.Open(self), 'socket)
  }

  override def postStop() {
    super.postStop()
    lilaBus.publish(lila.socket.SocketHub.Close(self), 'socket)
    members foreachKey eject
  }

  // to be defined in subclassing actor
  def receiveSpecific: Receive

  // generic message handler
  def receiveGeneric: Receive = {

    case Ping(uid) => ping(uid)

    case Broom => broom

    // when a member quits
    case Quit(uid) => quit(uid)

    case HasUserId(userId) => sender ! members.values.exists(_.userId.contains(userId))

    case Resync(uid) => resync(uid)

    case d: Deploy => onDeploy(d)
  }

  def receive = receiveSpecific orElse receiveGeneric

  def notifyAll[A: Writes](t: String, data: A): Unit =
    notifyAll(makeMessage(t, data))

  def notifyAll(t: String): Unit =
    notifyAll(makeMessage(t))

  def notifyAll(msg: JsObject): Unit =
    members.foreachValue(_ push msg)

  def notifyIf(msg: JsObject)(condition: M => Boolean): Unit =
    members.foreachValue { member =>
      if (condition(member)) member push msg
    }

  def notifyMember[A: Writes](t: String, data: A)(member: M) {
    member push makeMessage(t, data)
  }

  def notifyUid[A: Writes](t: String, data: A)(uid: Socket.Uid) {
    withMember(uid.value)(_ push makeMessage(t, data))
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

  def onDeploy(d: Deploy) {
    notifyAll(makeMessage(d.key))
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

  def membersByUserId(userId: String): Iterable[M] = members collect {
    case (_, member) if member.userId.contains(userId) => member
  }

  def firstMemberByUserId(userId: String): Option[M] = members collectFirst {
    case (_, member) if member.userId.contains(userId) => member
  }

  def uidToUserId(uid: Socket.Uid): Option[String] = members get uid.value flatMap (_.userId)

  val maxSpectatorUsers = 10

  def showSpectators(lightUser: LightUser.Getter)(watchers: Iterable[SocketMember]): Fu[JsValue] = {

    val (total, anons, userIds) = watchers.foldLeft((0, 0, Set.empty[String])) {
      case ((total, anons, userIds), member) => member.userId match {
        case Some(userId) if !userIds(userId) && userIds.size < maxSpectatorUsers => (total + 1, anons, userIds + userId)
        case Some(_) => (total + 1, anons, userIds)
        case _ => (total + 1, anons + 1, userIds)
      }
    }

    if (total == 0) fuccess(JsNull)
    else if (userIds.size >= maxSpectatorUsers) fuccess(Json.obj("nb" -> total))
    else userIds.map(lightUser).sequenceFu.map { users =>
      Json.obj(
        "nb" -> total,
        "users" -> users.flatten.map(_.titleName),
        "anons" -> anons
      )
    }
  }

  def withMember(uid: String)(f: M => Unit) {
    members get uid foreach f
  }
}
