package lila.lobby

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

import actorApi._
import lila.common.PimpedJson._
import lila.game.{ Game, AnonCookie }
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.lobby._
import lila.hub.actorApi.timeline._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.SocketActor

private[lobby] final class Socket(
    uidTtl: FiniteDuration) extends SocketActor[Member](uidTtl) {

  override val startsOnApplicationBoot = true

  case object Cleanup

  override def preStart() {
    super.preStart()
    context.system.lilaBus.subscribe(self, 'changeFeaturedGame, 'streams, 'nbMembers, 'nbRounds, 'poolGame)
    context.system.scheduler.scheduleOnce(3 seconds, self, SendHookRemovals)
    context.system.scheduler.schedule(1 minute, 1 minute, self, Cleanup)
  }

  override def postStop() {
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
      sender ! SocketUids(members.keySet.toSet)
      lila.mon.lobby.socket.idle(idleUids.size)
      lila.mon.lobby.socket.hookSubscribers(hookSubscriberUids.size)
      lila.mon.lobby.socket.mobile(members.count(_._2.mobile))

    case Cleanup =>
      idleUids retain members.contains
      hookSubscriberUids retain members.contains

    case Join(uid, user, blocks, mobile) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, blocks, uid.value, mobile)
      addMember(uid.value, member)
      sender ! Connected(enumerator, member)

    case ReloadTournaments(html) => notifyAllActiveAsync(makeMessage("tournaments", html))

    case ReloadSimuls(html)      => notifyAllActiveAsync(makeMessage("simuls", html))

    case NewForumPost            => notifyAllActiveAsync(makeMessage("reload_forum"))

    case ReloadTimeline(userId) =>
      membersByUserId(userId) foreach (_ push makeMessage("reload_timeline"))

    case AddHook(hook) => Future {
      val msg = makeMessage("had", hook.render)
      hookSubscriberUids.foreach { uid =>
        withActiveMember(uid) { member =>
          if (Biter.showHookTo(hook, member)) member push msg
        }
      }
      if (hook.likePoolFiveO) withMember(hook.uid) { member =>
        lila.mon.lobby.hook.createdLikePoolFiveO(member.mobile)()
      }
    }

    case AddSeek(_) => notifySeeks

    case RemoveHook(hookId) =>
      removedHookIds = s"$removedHookIds$hookId"

    case SendHookRemovals =>
      if (removedHookIds.nonEmpty) {
        val msg = makeMessage("hrm", removedHookIds)
        hookSubscriberUids.foreach { uid =>
          withActiveMember(uid)(_ push msg)
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

    case lila.pool.PoolApi.Pairing(game, whiteUid, blackUid) =>
      withMember(whiteUid.value)(notifyPlayerStart(game, chess.White))
      withMember(blackUid.value)(notifyPlayerStart(game, chess.Black))

    case HookIds(ids) => Future {
      val msg = makeMessage("hli", ids mkString "")
      hookSubscriberUids.foreach { uid =>
        withActiveMember(uid)(_ push msg)
      }
    }

    case lila.hub.actorApi.StreamsOnAir(html) => notifyAllAsync(makeMessage("streams", html))

    case NbMembers(nb)                        => pong = pong + ("d" -> JsNumber(nb))
    case lila.hub.actorApi.round.NbRounds(nb) =>
      pong = pong + ("r" -> JsNumber(nb))

    case ChangeFeatured(_, msg) => notifyAllActiveAsync(msg)

    case SetIdle(uid, true)     => idleUids += uid
    case SetIdle(uid, false)    => idleUids -= uid

    case HookSub(member, false) => hookSubscriberUids -= member.uid
    case AllHooksFor(member, hooks) =>
      notifyMember("hooks", JsArray(hooks.map(_.render)))(member)
      hookSubscriberUids += member.uid
  }

  def notifyPlayerStart(game: Game, color: chess.Color) =
    notifyMember("redirect", Json.obj(
      "id" -> (game fullIdOf color),
      "url" -> playerUrl(game fullIdOf color),
      "cookie" -> AnonCookie.json(game, color)
    ).noNull) _

  def notifyAllActiveAsync(msg: JsObject) = Future {
    members.foreach {
      case (uid, member) => if (!idleUids(uid)) member push msg
    }
  }

  def withActiveMember(uid: String)(f: Member => Unit) {
    if (!idleUids(uid)) members get uid foreach f
  }

  override def quit(uid: String) {
    super.quit(uid)
    idleUids -= uid
    hookSubscriberUids -= uid
  }

  def playerUrl(fullId: String) = s"/$fullId"

  def notifySeeks =
    notifyAllActiveAsync(makeMessage("reload_seeks"))
}
