package lila.socket

import actorApi._
import lila.hub.actorApi.{ GetUids, WithUserIds, GetNbMembers, NbMembers, SendTo, SendTos }
import lila.hub.actorApi.friend.FriendsOf
import lila.memo.ExpireSetMemo

import akka.actor._
import play.api.libs.json._
import scala.util.Random
import scala.concurrent.duration.Duration

abstract class SocketActor[M <: SocketMember](uidTtl: Duration) extends Actor {

  var members = Map.empty[String, M]
  val aliveUids = new ExpireSetMemo(uidTtl)
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

    case WithUserIds(f)             ⇒ f(userIds)

    case GetUids                    ⇒ sender ! uids

    case LiveGames(uid, gameIds)    ⇒ registerLiveGames(uid, gameIds)

    case Fen(gameId, fen, lastMove) ⇒ notifyFen(gameId, fen, lastMove)

    case SendTo(userId, msg)        ⇒ sendTo(userId, msg)

    case SendTos(userIds, msg)      ⇒ sendTos(userIds, msg)

    case FriendsOf(uid, friends)    ⇒ withMember(uid)(notifyMember("friends", friends))

    case Resync(uid)                ⇒ resync(uid)
  }

  def receive = receiveSpecific orElse receiveGeneric

  def notifyAll[A: Writes](t: String, data: A) {
    val msg = makeMessage(t, data)
    members.values.foreach(_.channel push msg)
  }

  def notifyMember[A: Writes](t: String, data: A)(member: M) {
    member.channel push makeMessage(t, data)
  }

  def makeMessage[A: Writes](t: String, data: A) =
    Json.obj("t" -> t, "d" -> data)

  def makePong(nb: Int) = makeMessage("n", nb)

  def ping(uid: String) {
    setAlive(uid)
    withMember(uid)(_.channel push pong)
  }

  def sendTo(userId: String, msg: JsObject) {
    memberByUserId(userId) foreach (_.channel push msg)
  }

  def sendTos(userIds: Set[String], msg: JsObject) {
    membersByUserIds(userIds) foreach (_.channel push msg)
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

  private lazy val resyncMessage = makeMessage("resync", JsNull)

  protected def resync(member: M) {
    import play.api.libs.concurrent._
    import play.api.Play.current
    import scala.concurrent.duration._

    Akka.system.scheduler.scheduleOnce((Random nextInt 3).seconds) {
      resyncNow(member)
    }
  }

  protected def resync(uid: String) {
    withMember(uid)(resync)
  }

  protected def resyncNow(member: M) {
    member.channel push resyncMessage
  }

  def addMember(uid: String, member: M) {
    eject(uid)
    members = members + (uid -> member)
    setAlive(uid)
  }

  def setAlive(uid: String) { aliveUids put uid }

  def uids = members.keys

  def memberByUserId(userId: String): Option[M] = {
    val someId = Some(userId)
    members.values find (_.userId == someId)
  }

  def membersByUserIds(userIds: Set[String]): Iterable[M] =
    members.values filter (member ⇒ member.userId ?? userIds.contains)

  def userIds: Iterable[String] = members.values.map(_.userId).flatten

  def notifyFen(gameId: String, fen: String, lastMove: Option[String]) {
    val msg = makeMessage("fen", JsObject(Seq(
      "id" -> JsString(gameId),
      "fen" -> JsString(fen),
      "lm" -> Json.toJson(lastMove)
    )))
    members.values filter (_ liveGames gameId) foreach (_.channel push msg)
  }

  def showSpectators(users: List[String], nbAnons: Int) = nbAnons match {
    case 0 ⇒ users
    case 1 ⇒ users :+ "Anonymous"
    case x ⇒ users :+ ("Anonymous (%d)" format x)
  }

  def registerLiveGames(uid: String, ids: List[String]) {
    withMember(uid)(_ addLiveGames ids)
  }

  def withMember(uid: String)(f: M ⇒ Unit) {
    members get uid foreach f
  }
}
