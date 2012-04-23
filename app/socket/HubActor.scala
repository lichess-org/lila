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

    case Ping(uid) ⇒ ping(uid)

    case Broom     ⇒ broom()

    case Quit(uid) ⇒ {
      members = members - uid
    }
  }

  def receive = receiveSpecific orElse receiveGeneric

  protected[this] def ping(uid: String) {
    setAlive(uid)
    member(uid) foreach (_.channel push Util.pong)
  }

  protected[this] def broom() {
    members foreach {
      case (uid, member) ⇒
        if (!(aliveUids get uid)) {
          member.channel.close()
          self ! Quit(uid)
        }
    }
  }

  def setAlive(uid: String) {
    aliveUids putUnsafe uid
  }

  def uids = members.keys

  def member(uid: String) = members get uid
}
