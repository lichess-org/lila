package lila
package socket

import akka.actor._
import akka.event.Logging

abstract class HubActor[M <: Channeled](timeout: Int) extends Actor {

  var members = Map.empty[String, M]
  val aliveUids = new PingMemo(timeout)
  val log = Logging(context.system, this)

  // to be defined in subclassing actor
  def receiveSpecific: Receive

  // generic message handler
  def receiveGeneric: Receive = {

    case Ping(uid) ⇒ {
      setAlive(uid)
      member(uid) foreach (_.channel push Util.pong)
    }

    case Broom ⇒ {
      uids filterNot aliveUids.get foreach { uid ⇒
        self ! Quit(uid)
      }
    }

    case Quit(uid) ⇒ {
      log.info("quit " + uid)
      members = members - uid
    }
  }

  def receive = receiveSpecific orElse receiveGeneric

  def setAlive(uid: String) {
    aliveUids putUnsafe uid
  }

  def uids = members.keys

  def member(uid: String) = members get uid
}
