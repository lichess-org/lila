package lila
package socket

import core.Global.env // fuck. need it for message unread cache

import akka.actor._
import play.api.libs.json._

abstract class HubActor[M <: SocketMember](uidTimeout: Int) extends Actor {

  var members = Map.empty[String, M]
  val aliveUids = new PingMemo(uidTimeout)
  var pong = makePong(0)

  // to be defined in subclassing actor
  def receiveSpecific: Receive

  // generic message handler
  def receiveGeneric: Receive = {

    case Ping(uid)     ⇒ ping(uid)

    case Broom         ⇒ broom()

    // when a member quits
    case Quit(uid)     ⇒ quit(uid)

    case GetNbMembers  ⇒ sender ! members.size

    case NbMembers(nb) ⇒ pong = makePong(nb)

    case GetUsernames  ⇒ sender ! usernames
  }

  def receive = receiveSpecific orElse receiveGeneric

  def notifyAll(t: String, data: JsValue) {
    val msg = makeMessage(t, data)
    members.values.foreach(_.channel push msg)
  }

  def makeMessage(t: String, data: JsValue) =
    JsObject(Seq("t" -> JsString(t), "d" -> data))

  def makePong(nb: Int) = makeMessage("n", JsNumber(nb))

  def ping(uid: String) {
    setAlive(uid)
    member(uid) foreach { m ⇒
      m.channel push m.username.fold(
        u ⇒ pong ++ JsObject(Seq("m" -> JsNumber(unreadMessages(u)))),
        pong)
    }
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

  def member(uid: String): Option[M] = members get uid

  def usernames: Iterable[String] = members.values.map(_.username).flatten

  private def unreadMessages(username: String): Int = 
    env.message.unreadCache get username
}
