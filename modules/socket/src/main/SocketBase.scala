package lila.socket

import scala.concurrent.duration._
import scala.util.Random

import akka.actor.ActorSystem
import play.api.libs.json._

import actorApi._
import chess.Centis
import lila.common.LightUser
import lila.hub.actorApi.Deploy
import lila.memo.ExpireSetMemo

trait SocketBase[M <: SocketMember] extends Socket {

  protected def uidTtl: Duration
  protected def system: ActorSystem

  protected val members = scala.collection.mutable.AnyRefMap.empty[String, M]
  protected val aliveUids = new ExpireSetMemo(uidTtl)
  protected var pong = initialPong

  protected def lilaBus = system.lilaBus

  // to be defined in subclassing socket
  protected def receiveSpecific: PartialFunction[Any, Unit]

  // generic message handler
  protected def receiveGeneric: PartialFunction[Any, Unit] = {

    case Ping(uid, _, lagCentis) => ping(uid, lagCentis)

    case Broom => broom

    // when a member quits
    case Quit(uid) => quit(uid)

    case Resync(uid) => resync(uid)

    case d: Deploy => onDeploy(d)
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

  protected def ping(uid: Socket.Uid, lagCentis: Option[Centis]): Unit = {
    setAlive(uid)
    withMember(uid) { member =>
      member push pong
      for {
        lc <- lagCentis
        user <- member.userId
      } UserLagCache.put(user, lc)
    }
  }

  protected def broom: Unit =
    members.keys foreach { uid =>
      if (!aliveUids.get(uid)) ejectUidString(uid)
    }

  protected def ejectUidString(uid: String): Unit = eject(Socket.Uid(uid))

  protected def eject(uid: Socket.Uid): Unit = withMember(uid) { member =>
    member.end
    quit(uid)
  }

  protected def quit(uid: Socket.Uid): Unit = withMember(uid) { member =>
    members -= uid.value
    lilaBus.publish(SocketLeave(uid, member), 'socketLeave)
  }

  protected def onDeploy(d: Deploy): Unit =
    notifyAll(makeMessage(d.key))

  protected val resyncMessage = makeMessage("resync")

  protected def resync(member: M): Unit = {
    import scala.concurrent.duration._
    system.scheduler.scheduleOnce((Random nextInt 2000).milliseconds) {
      resyncNow(member)
    }
  }

  protected def resync(uid: Socket.Uid): Unit =
    withMember(uid)(resync)

  protected def resyncNow(member: M): Unit =
    member push resyncMessage

  protected def addMember(uid: Socket.Uid, member: M): Unit = {
    eject(uid)
    members += (uid.value -> member)
    setAlive(uid)
    lilaBus.publish(SocketEnter(uid, member), 'socketEnter)
  }

  protected def setAlive(uid: Socket.Uid): Unit = aliveUids put uid.value

  protected def membersByUserId(userId: String): Iterable[M] = members collect {
    case (_, member) if member.userId.contains(userId) => member
  }

  protected def firstMemberByUserId(userId: String): Option[M] = members collectFirst {
    case (_, member) if member.userId.contains(userId) => member
  }

  protected def uidToUserId(uid: Socket.Uid): Option[String] = members get uid.value flatMap (_.userId)

  protected val maxSpectatorUsers = 15

  protected def showSpectators(lightUser: LightUser.Getter)(watchers: Iterable[SocketMember]): Fu[JsValue] = watchers.size match {
    case 0 => fuccess(JsNull)
    case s if s > maxSpectatorUsers => fuccess(Json.obj("nb" -> s))
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
        )
      }
    }
  }

  protected def withMember(uid: Socket.Uid)(f: M => Unit): Unit = members get uid.value foreach f
}
