package lila.socket

import scala.concurrent.duration._
import scala.util.Random

import akka.actor.{ Deploy => _, _ }
import play.api.libs.json._
import play.api.templates.Html

import actorApi._
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.round.MoveEvent
import lila.hub.actorApi.{ Deploy, GetUids, WithUserIds, SendTo, SendTos }
import lila.memo.ExpireSetMemo

abstract class SocketActor[M <: SocketMember](uidTtl: Duration) extends Socket with Actor {

  var members = Map.empty[String, M]
  val aliveUids = new ExpireSetMemo(uidTtl)
  var pong = makePong(0)

  val lilaBus = context.system.lilaBus

  lilaBus.publish(lila.socket.SocketHub.Subscribe(self), 'socket)

  override def postStop() {
    lilaBus.publish(lila.socket.SocketHub.Unsubscribe(self), 'socket)
    members.keys foreach eject
  }

  // to be defined in subclassing actor
  def receiveSpecific: Receive

  // generic message handler
  def receiveGeneric: Receive = {

    case Ping(uid)               => ping(uid)

    case Broom                   => broom

    // when a member quits
    case Quit(uid)               => quit(uid)

    case NbMembers(nb)           => pong = makePong(nb)

    case WithUserIds(f)          => f(userIds)

    case GetUids                 => sender ! uids

    case LiveGames(uid, gameIds) => registerLiveGames(uid, gameIds)

    case move: MoveEvent         => notifyMove(move)

    case SendTo(userId, msg)     => sendTo(userId, msg)

    case SendTos(userIds, msg)   => sendTos(userIds, msg)

    case Resync(uid)             => resync(uid)

    case Deploy(event, html)     => notifyAll(makeMessage(event.key, html))

    // the actor instance must subscribe to 'changeFeaturedGame to receive this message
    // context.system.lilaBus.subscribe(self, 'changeFeaturedGame)
    case ChangeFeatured(_, html) => notifyFeatured(html)
  }

  def receive = receiveSpecific orElse receiveGeneric

  def notifyAll[A: Writes](t: String, data: A) {
    notifyAll(makeMessage(t, data))
  }

  def notifyAll(t: String) {
    notifyAll(makeMessage(t))
  }

  def notifyAll(msg: JsObject) {
    members.values.foreach(_.channel push msg)
  }

  def notifyMember[A: Writes](t: String, data: A)(member: M) {
    member.channel push makeMessage(t, data)
  }

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

  def broom {
    members.keys filterNot aliveUids.get foreach eject
  }

  def eject(uid: String) {
    withMember(uid) { member =>
      member.channel.end()
      quit(uid)
    }
  }

  def quit(uid: String) {
    if (members contains uid) {
      members = members - uid
      lilaBus.publish(SocketLeave(uid), 'socketDoor)
    }
  }

  private val resyncMessage = makeMessage("resync", JsNull)

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
    member.channel push resyncMessage
  }

  def addMember(uid: String, member: M) {
    eject(uid)
    members = members + (uid -> member)
    setAlive(uid)
    lilaBus.publish(SocketEnter(uid, member), 'socketDoor)
  }

  def setAlive(uid: String) { aliveUids put uid }

  def uids = members.keys

  def memberByUserId(userId: String): Option[M] =
    members.values find (_.userId == Some(userId))

  def membersByUserIds(userIds: Set[String]): Iterable[M] =
    members.values filter (member => member.userId ?? userIds.contains)

  def userIds: Iterable[String] = members.values.map(_.userId).flatten

  def notifyMove(move: MoveEvent) {
    lazy val msg = makeMessage("fen", Json.obj(
      "id" -> move.gameId,
      "fen" -> move.fen,
      "lm" -> move.move
    ))
    members.values foreach { m =>
      if (m liveGames move.gameId) m.channel push msg
    }
  }

  def showSpectators(users: List[lila.common.LightUser], nbAnons: Int) = nbAnons match {
    case 0 => users.distinct.map(_.titleName)
    case x => users.distinct.map(_.titleName) :+ ("Anonymous (%d)" format x)
  }

  def registerLiveGames(uid: String, ids: List[String]) {
    withMember(uid)(_ addLiveGames ids)
  }

  def withMember(uid: String)(f: M => Unit) {
    members get uid foreach f
  }

  private def notifyFeatured(html: Html) {
    val msg = makeMessage("featured", Json.obj("html" -> html.toString))
    members.values foreach { _.channel push msg }
  }
}
