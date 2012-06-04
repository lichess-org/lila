package lila
package socket

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

    case Ping(uid)            ⇒ ping(uid)

    case Broom                ⇒ broom()

    // when a member quits
    case Quit(uid)            ⇒ quit(uid)

    case GetNbMembers         ⇒ sender ! members.size

    case NbMembers(nb)        ⇒ pong = makePong(nb)

    case GetUsernames         ⇒ sender ! usernames

    case SendTo(userId, msg) ⇒ sendTo(userId, msg)
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
    member(uid) foreach (_.channel push pong)
  }

  def sendTo(userId: String, msg: JsObject) {
    memberByUserId(userId) foreach (_.channel push msg)
  }

  def broom() {
    members.keys filterNot aliveUids.get foreach eject
  }

  def eject(uid: String) {
    members get uid foreach { member ⇒
      member.channel.end()
      quit(uid)
    }
  }

  def quit(uid: String) {
    members = members - uid
  }

  def resync(member: M) {
    member.channel push JsObject(Seq(
      "t" -> JsString("resync")
    ))
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

  def memberByUserId(userId: String): Option[M] = {
    val someId = Some(userId)
    members.values find (_.userId == someId)
  }

  def usernames: Iterable[String] = members.values.map(_.username).flatten
}
