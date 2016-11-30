package lila.lobby

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.pattern.ask
import play.api.libs.iteratee._
import play.api.libs.json._
import play.twirl.api.Html

import actorApi._
import lila.common.PimpedJson._
import lila.game.actorApi._
import lila.game.AnonCookie
import lila.hub.actorApi.game.ChangeFeatured
import lila.hub.actorApi.lobby._
import lila.hub.actorApi.timeline._
import lila.socket.actorApi.{ SocketLeave, Connected => _, _ }
import lila.socket.SocketActor
import makeTimeout.short

private[lobby] final class Socket(
    uidTtl: FiniteDuration) extends SocketActor[Member](uidTtl) {

  override val startsOnApplicationBoot = true

  override def preStart() {
    super.preStart()
    context.system.lilaBus.subscribe(self, 'changeFeaturedGame, 'streams, 'nbMembers, 'nbRounds)
  }

  override def postStop() {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  var idleUids = scala.collection.mutable.Set[String]()

  def receiveSpecific = {

    case GetUids => sender ! SocketUids(members.keySet.toSet)

    case Join(uid, user, blocks, mobile) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, blocks, uid, mobile)
      addMember(uid, member)
      sender ! Connected(enumerator, member)

    case ReloadTournaments(html) => notifyAllActiveAsync(makeMessage("tournaments", html))

    case ReloadSimuls(html)      => notifyAllActiveAsync(makeMessage("simuls", html))

    case NewForumPost            => notifyAllActiveAsync(makeMessage("reload_forum"))

    case ReloadTimeline(userId) =>
      membersByUserId(userId) foreach (_ push makeMessage("reload_timeline"))

    case AddHook(hook) => Future {
      val json = hook.render
      members.foreach {
        case (uid, member) =>
          if (!member.mobile && !idleUids(uid)) notifyMemberOfHook(member, hook, json)
      }
    }

    case AddSeek(_) => notifySeeks

    case RemoveHook(hookId) => Future {
      val msg = makeMessage("hrm", hookId)
      members.foreach {
        case (uid, member) =>
          if (!member.mobile && !idleUids(uid)) member push msg
      }
    }

    case RemoveSeek(_) => notifySeeks

    case JoinHook(uid, hook, game, creatorColor) =>
      withMember(hook.uid)(notifyPlayerStart(game, creatorColor))
      withMember(uid)(notifyPlayerStart(game, !creatorColor))

    case JoinSeek(userId, seek, game, creatorColor) =>
      membersByUserId(seek.user.id) foreach notifyPlayerStart(game, creatorColor)
      membersByUserId(userId) foreach notifyPlayerStart(game, !creatorColor)

    case HookIds(ids)                         => Future {
      val msg = makeMessage("hli", ids mkString ",")
      members.foreach {
        case (uid, member) =>
          if (!member.mobile && !idleUids(uid)) member push msg
      }
    }
    case NbHooks(count)                       => notifyAllAsync(makeMessage("nb_hooks", count))

    case lila.hub.actorApi.StreamsOnAir(html) => notifyAllAsync(makeMessage("streams", html))

    case NbMembers(nb)                        => pong = pong + ("d" -> JsNumber(nb))
    case lila.hub.actorApi.round.NbRounds(nb) =>
      pong = pong + ("r" -> JsNumber(nb))

    case ChangeFeatured(_, msg) => notifyAllActiveAsync(msg)

    case SetIdle(uid, true)     => idleUids += uid
    case SetIdle(uid, false)    => idleUids -= uid
  }

  private def notifyPlayerStart(game: lila.game.Game, color: chess.Color) =
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

  def notifyMemberOfHook(member: Member, hook: Hook, json: JsObject) =
    if (hook.uid == member.uid || Biter.canJoin(hook, member.user))
      member push makeMessage("had", json)

  def withActiveMember(uid: String)(f: Member => Unit) {
    if (!idleUids(uid)) members get uid foreach f
  }

  override def quit(uid: String) {
    super.quit(uid)
    idleUids -= uid
  }

  private def playerUrl(fullId: String) = s"/$fullId"

  private def notifySeeks =
    notifyAll(makeMessage("reload_seeks"))
}
