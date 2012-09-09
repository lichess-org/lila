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

    case Ping(uid)                  ⇒ ping(uid)

    case Broom                      ⇒ broom()

    // when a member quits
    case Quit(uid)                  ⇒ quit(uid)

    case GetNbMembers               ⇒ sender ! members.size

    case NbMembers(nb)              ⇒ pong = makePong(nb)

    case GetUsernames               ⇒ sender ! usernames

    case LiveGames(uid, gameIds)    ⇒ registerLiveGames(uid, gameIds)

    case Fen(gameId, fen, lastMove) ⇒ notifyFen(gameId, fen, lastMove)

    case SendTo(userId, msg)        ⇒ sendTo(userId, msg)

    case Resync(uid)                ⇒ resync(uid)
  }

  def receive = receiveSpecific orElse receiveGeneric

  def notifyAll(t: String, data: JsValue) {
    val msg = makeMessage(t, data)
    members.values.foreach(_.channel push msg)
  }

  def notifyMember(t: String, data: JsValue)(member: M) {
    member.channel push makeMessage(t, data)
  }

  def makeMessage(t: String, data: JsValue) =
    JsObject(Seq("t" -> JsString(t), "d" -> data))

  def makePong(nb: Int) = makeMessage("n", JsNumber(nb))

  def ping(uid: String) {
    setAlive(uid)
    withMember(uid)(_.channel push pong)
  }

  def sendTo(userId: String, msg: JsObject) {
    memberByUserId(userId) foreach (_.channel push msg)
  }

  def broom() {
    members.keys filterNot aliveUids.get foreach eject
  }

  def eject(uid: String) {
    withMember(uid) { member ⇒
      member.channel.end()
      quit(uid)
    }
  }

  def quit(uid: String) {
    members = members - uid
  }

  def resync(member: M) {
    member.channel push makeMessage("resync", JsNull)
  }

  def resync(uid: String) {
    withMember(uid)(resync)
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

  def memberByUserId(userId: String): Option[M] = {
    val someId = Some(userId)
    members.values find (_.userId == someId)
  }

  def usernames: Iterable[String] = members.values.map(_.username).flatten

  def notifyFen(gameId: String, fen: String, lastMove: Option[String]) {
    val msg = makeMessage("fen", JsObject(Seq(
      "id" -> JsString(gameId),
      "fen" -> JsString(fen),
      "lm" -> lastMove.fold(JsString(_), JsNull)
    )))
    members.values filter (_ liveGames gameId) foreach (_.channel push msg)
  }

  def registerLiveGames(uid: String, ids: List[String]) {
    withMember(uid)(_ addLiveGames ids)
  }

  def withMember(uid: String)(f: M ⇒ Unit) {
    members get uid foreach f
  }
}
