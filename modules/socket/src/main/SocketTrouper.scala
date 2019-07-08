package lila.socket

import ornicar.scalalib.Random.approximatly
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import actorApi._
import chess.Centis
import lila.common.LightUser
import lila.hub.actorApi.{ Deploy, Announce }
import lila.hub.actorApi.socket.HasUserId
import lila.hub.Trouper
import lila.memo.ExpireSetMemo

abstract class SocketTrouper[M <: SocketMember](
    protected val system: akka.actor.ActorSystem,
    protected val uidTtl: Duration
) extends Socket with Trouper {

  import SocketTrouper._

  /* Do not eject members on stop!
   * It does not always instruct the client to disconnect (!)
   * But it does prevent from sending it more messages.
   * If the client isn't disconnected, we can't tell it to resync
   * when we receive more messages from it!
   * In theory a socket should only stop when all clients are gone anyway.
   */
  // override def stop() = {
  //   super.stop()
  //   members foreachKey ejectUidString
  // }

  protected val receiveTrouper: PartialFunction[Any, Unit] = {

    case HasUserId(userId, promise) => promise success hasUserId(userId)

    case GetNbMembers(promise) => promise success members.size
  }

  val process = receiveSpecific orElse receiveTrouper orElse receiveGeneric

  // expose so the handler can call without going through process, during ping
  def setAlive(uid: Socket.Uid): Unit = aliveUids put uid.value

  protected val members = scala.collection.mutable.AnyRefMap.empty[String, M]
  protected val aliveUids = new ExpireSetMemo(uidTtl)

  protected def lilaBus = system.lilaBus

  // to be defined in subclassing socket
  protected def receiveSpecific: PartialFunction[Any, Unit]

  // generic message handler
  protected def receiveGeneric: PartialFunction[Any, Unit] = {

    case Broom => broom

    // when a member quits
    case Quit(uid, member) => withMember(uid) { m =>
      if (m eq member) quit(uid, m)
    }

    case Resync(uid) => resync(uid)

    case d: Deploy => onDeploy(d)

    case Announce(msg) => notifyAll(makeMessage("announce", Json.obj("msg" -> msg)))
  }

  protected def hasUserId(userId: String) = members.values.exists(_.userId contains userId)

  protected def notifyAll[A: Writes](t: String, data: A): Unit =
    notifyAll(makeMessage(t, data))

  protected def notifyAll(t: String): Unit =
    notifyAll(makeMessage(t))

  protected def notifyAll(msg: JsObject): Unit =
    members.foreachValue(_ push msg)

  protected def notifyIf(msg: JsObject)(condition: M => Boolean): Unit =
    members.foreachValue { member =>
      if (condition(member)) member push msg
    }

  protected def notifyMember[A: Writes](t: String, data: A)(member: M): Unit = {
    member push makeMessage(t, data)
  }

  protected def notifyUid[A: Writes](t: String, data: A)(uid: Socket.Uid): Unit = {
    withMember(uid)(_ push makeMessage(t, data))
  }

  protected def broom: Unit =
    members foreachKey { uid =>
      if (!aliveUids.get(uid)) ejectUidString(uid)
    }

  protected def ejectUidString(uid: String): Unit = eject(Socket.Uid(uid))

  // actively boot a member, if it exists
  // this function is called when a member joins,
  // to prevent duplicate UID
  private final def eject(uid: Socket.Uid): Unit = withMember(uid) { member =>
    member.end
    quit(uid, member)
  }

  // when a member quits, voluntarily or not
  // at this point we know the member exists
  private final def quit(uid: Socket.Uid, member: M): Unit = {
    members -= uid.value
    lilaBus.publish(SocketLeave(uid, member), 'socketLeave)
    afterQuit(uid, member)
  }

  protected def afterQuit(uid: Socket.Uid, member: M): Unit = {}

  protected def onDeploy(d: Deploy): Unit =
    notifyAll(makeMessage(d.key))

  protected def resync(member: M): Unit = {
    import scala.concurrent.duration._
    system.scheduler.scheduleOnce((scala.util.Random nextInt 2000).milliseconds) {
      resyncNow(member)
    }
  }

  protected def resync(uid: Socket.Uid): Unit =
    withMember(uid)(resync)

  def resyncNow(member: M): Unit =
    member push resyncMessage

  protected def addMember(uid: Socket.Uid, member: M): Unit = {
    eject(uid)
    members += (uid.value -> member)
    setAlive(uid)
    lilaBus.publish(SocketEnter(uid, member), 'socketEnter)
  }

  protected def membersByUserId(userId: String): Iterable[M] = members collect {
    case (_, member) if member.userId.contains(userId) => member
  }

  protected def firstMemberByUserId(userId: String): Option[M] = members collectFirst {
    case (_, member) if member.userId.contains(userId) => member
  }

  protected def uidToUserId(uid: Socket.Uid): Option[String] = members get uid.value flatMap (_.userId)

  protected val maxSpectatorUsers = 15

  protected def showSpectators(lightUser: LightUser.Getter)(watchers: Iterable[SocketMember]): Fu[Option[JsValue]] = watchers.size match {
    case 0 => fuccess(none)
    case s if s > maxSpectatorUsers => fuccess(Json.obj("nb" -> s).some)
    case s => {
      val userIdsWithDups = watchers.toSeq.flatMap(_.userId)
      val anons = s - userIdsWithDups.size
      val userIds = userIdsWithDups.distinct

      val total = anons + userIds.size

      userIds.map(lightUser).sequenceFu.map { users =>
        Json.obj(
          "nb" -> total,
          "users" -> users.flatten.map(_.titleName),
          "anons" -> anons
        ).some
      }
    }
  }

  protected def withMember(uid: Socket.Uid)(f: M => Unit): Unit = members get uid.value foreach f
}

object SocketTrouper extends Socket {
  case class GetNbMembers(promise: Promise[Int])

  val resyncMessage = makeMessage("resync")

  def resyncMsgWithDebug(debug: => String) =
    if (Env.current.socketDebugSetting.get) makeMessageDebug("resync", debug)
    else resyncMessage
}

// Not managed by a TrouperMap
trait LoneSocket { self: SocketTrouper[_] =>

  def monitoringName: String
  def broomFrequency: FiniteDuration

  system.scheduler.schedule(approximatly(0.1f)(12.seconds.toMillis).millis, broomFrequency) {
    this ! lila.socket.actorApi.Broom
    lila.mon.socket.queueSize(monitoringName)(queueSize)
  }
  system.lilaBus.subscribe(this, 'deploy, 'announce)
}
