package lila.lobby

import scala.concurrent.duration._

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.game.{ Game, AnonCookie }
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.lobby._
import lila.hub.actorApi.timeline._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Socket.{ Uid, Uids }
import lila.socket.SocketActor

private[lobby] final class Socket(
    uidTtl: FiniteDuration
) extends SocketActor[Member](uidTtl) {

  override val startsOnApplicationBoot = true

  case object Cleanup

  override def preStart(): Unit = {
    super.preStart()
    context.system.lilaBus.subscribe(self, 'changeFeaturedGame, 'streams, 'nbMembers, 'nbRounds, 'poolGame)
    context.system.scheduler.scheduleOnce(3 seconds, self, SendHookRemovals)
    context.system.scheduler.schedule(1 minute, 1 minute, self, Cleanup)
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  var idleUids = scala.collection.mutable.Set[String]()

  var hookSubscriberUids = scala.collection.mutable.Set[String]()

  var removedHookIds = ""

  def receiveSpecific = {

    case GetUids =>
      sender ! Uids(members.keySet.map(Uid.apply)(scala.collection.breakOut))
      lila.mon.lobby.socket.idle(idleUids.size)
      lila.mon.lobby.socket.hookSubscribers(hookSubscriberUids.size)
      lila.mon.lobby.socket.mobile(members.count(_._2.mobile))

    case Cleanup =>
      idleUids retain members.contains
      hookSubscriberUids retain members.contains

    case Join(uid, user, blocks, mobile) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, blocks, uid, mobile)
      addMember(uid, member)
      sender ! Connected(enumerator, member)

    case ReloadTournaments(html) => notifyAllActive(makeMessage("tournaments", html))

    case ReloadSimuls(html) => notifyAllActive(makeMessage("simuls", html))

    case ReloadTimeline(userId) =>
      membersByUserId(userId) foreach (_ push makeMessage("reload_timeline"))

    case AddHook(hook) =>
      val msg = makeMessage("had", hook.render)
      hookSubscriberUids.foreach { uid =>
        withActiveMemberByUidString(uid) { member =>
          if (Biter.showHookTo(hook, member)) member push msg
        }
      }
      if (hook.likePoolFiveO) withMember(hook.uid) { member =>
        lila.mon.lobby.hook.createdLikePoolFiveO(member.mobile)()
      }

    case AddSeek(_) => notifySeeks

    case RemoveHook(hookId) =>
      removedHookIds = s"$removedHookIds$hookId"

    case SendHookRemovals =>
      if (removedHookIds.nonEmpty) {
        val msg = makeMessage("hrm", removedHookIds)
        hookSubscriberUids.foreach { uid =>
          withActiveMemberByUidString(uid)(_ push msg)
        }
        removedHookIds = ""
      }
      context.system.scheduler.scheduleOnce(1 second, self, SendHookRemovals)

    case RemoveSeek(_) => notifySeeks

    case JoinHook(uid, hook, game, creatorColor) =>
      withMember(hook.uid) { member =>
        lila.mon.lobby.hook.joinMobile(member.mobile)()
        notifyPlayerStart(game, creatorColor)(member)
      }
      withMember(uid) { member =>
        lila.mon.lobby.hook.joinMobile(member.mobile)()
        if (hook.likePoolFiveO)
          lila.mon.lobby.hook.acceptedLikePoolFiveO(member.mobile)()
        notifyPlayerStart(game, !creatorColor)(member)
      }

    case JoinSeek(userId, seek, game, creatorColor) =>
      membersByUserId(seek.user.id) foreach { member =>
        lila.mon.lobby.seek.joinMobile(member.mobile)()
        notifyPlayerStart(game, creatorColor)(member)
      }
      membersByUserId(userId) foreach { member =>
        lila.mon.lobby.seek.joinMobile(member.mobile)()
        notifyPlayerStart(game, !creatorColor)(member)
      }

    case pairing: lila.pool.PoolApi.Pairing =>
      def goPlayTheGame = redirectPlayers(pairing)
      goPlayTheGame // go play the game now
      context.system.scheduler.scheduleOnce(1 second)(goPlayTheGame) // I said go
      context.system.scheduler.scheduleOnce(3 second)(goPlayTheGame) // Darn it

    case HookIds(ids) =>
      val msg = makeMessage("hli", ids mkString "")
      hookSubscriberUids.foreach { uid =>
        withActiveMemberByUidString(uid)(_ push msg)
      }

    case lila.hub.actorApi.streamer.StreamsOnAir(html) => notifyAll(makeMessage("streams", html))

    case NbMembers(nb) => pong = pong + ("d" -> JsNumber(nb))
    case lila.hub.actorApi.round.NbRounds(nb) =>
      pong = pong + ("r" -> JsNumber(nb))

    case ChangeFeatured(_, msg) => notifyAllActive(msg)

    case SetIdle(uid, true) => idleUids += uid.value
    case SetIdle(uid, false) => idleUids -= uid.value

    case HookSub(member, false) => hookSubscriberUids -= member.uid.value
    case AllHooksFor(member, hooks) =>
      notifyMember("hooks", JsArray(hooks.map(_.render)))(member)
      hookSubscriberUids += member.uid.value
  }

  def redirectPlayers(p: lila.pool.PoolApi.Pairing) = {
    withMember(p.whiteUid)(notifyPlayerStart(p.game, chess.White))
    withMember(p.blackUid)(notifyPlayerStart(p.game, chess.Black))
  }

  def notifyPlayerStart(game: Game, color: chess.Color) =
    notifyMember("redirect", Json.obj(
      "id" -> (game fullIdOf color),
      "url" -> playerUrl(game fullIdOf color)
    ).add("cookie" -> AnonCookie.json(game, color))) _

  def notifyAllActive(msg: JsObject) =
    members.foreach {
      case (uid, member) => if (!idleUids(uid)) member push msg
    }

  private def withActiveMemberByUidString(uid: String)(f: Member => Unit): Unit =
    if (!idleUids(uid)) members get uid foreach f

  override def quit(uid: Uid): Unit = {
    super.quit(uid)
    idleUids -= uid.value
    hookSubscriberUids -= uid.value
  }

  def playerUrl(fullId: String) = s"/$fullId"

  def notifySeeks = notifyAllActive(makeMessage("reload_seeks"))
}
