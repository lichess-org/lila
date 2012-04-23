package lila
package socket

import akka.actor._
import akka.event.Logging

abstract class HubActor[M <: Channeled](uidTimeout: Int) extends Actor {

  var members = Map.empty[String, M]
  val aliveUids = new PingMemo(uidTimeout)
  val log = Logging(context.system, this)

  // to be defined in subclassing actor
  def receiveSpecific: Receive

  // generic message handler
  def receiveGeneric: Receive = {

    case Ping(uid)    ⇒ ping(uid)

    case Broom        ⇒ broom()

    // when a member quits
    case Quit(uid)    ⇒ quit(uid)

    case GetNbMembers ⇒ sender ! members.size
  }

  def receive = receiveSpecific orElse receiveGeneric

  def ping(uid: String) {
    setAlive(uid)
    member(uid) foreach (_.channel push Util.pong)
  }

  def broom() {
    members.keys filterNot aliveUids.get foreach eject
  }

  def eject(uid: String) {
    members get uid foreach { member ⇒
      member.channel.close()
      quit(uid)
    }
  }

  def quit(uid: String) {
    members = members - uid
  }

  def addMember(uid: String, member: M) {
    eject(uid)
    members = members + (uid -> member)
    setAlive(uid)
  }

  def setAlive(uid: String) {
    aliveUids putUnsafe uid
  }

  def uids = members.keys

  def member(uid: String) = members get uid
}
