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
import lila.socket.{ SocketActor, History, Historical }
import makeTimeout.short

private[lobby] final class Socket(
    val history: History[Messadata],
    uidTtl: FiniteDuration) extends SocketActor[Member](uidTtl) with Historical[Member, Messadata] {

  override val startsOnApplicationBoot = true

  override def preStart() {
    super.preStart()
    context.system.lilaBus.subscribe(self, 'changeFeaturedGame, 'streams, 'nbMembers, 'nbRounds, 'socketDoor)
  }

  override def postStop() {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  var idleUids = scala.collection.mutable.Set[String]()

  def receiveSpecific = {

    case PingVersion(uid, v) => Future {
      ping(uid)
      withMember(uid) { m =>
        history.since(v).fold {
          lila.mon.lobby.socket.resync()
          resync(m)
        }(_ foreach sendMessage(m))
      }
    }

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

    case AddHook(hook) =>
      notifyVersion("had", hook.render, Messadata(hook = hook.some))

    case AddSeek(_)         => notifySeeks

    case RemoveHook(hookId) => notifyVersion("hrm", hookId, Messadata())

    case RemoveSeek(_)      => notifySeeks

    case JoinHook(uid, hook, game, creatorColor) =>
      withMember(hook.uid)(notifyPlayerStart(game, creatorColor))
      withMember(uid)(notifyPlayerStart(game, !creatorColor))

    case JoinSeek(userId, seek, game, creatorColor) =>
      membersByUserId(seek.user.id) foreach notifyPlayerStart(game, creatorColor)
      membersByUserId(userId) foreach notifyPlayerStart(game, !creatorColor)

    case HookIds(ids)                         => notifyVersion("hli", ids mkString ",", Messadata())

    case lila.hub.actorApi.StreamsOnAir(html) => notifyAllAsync(makeMessage("streams", html))

    case NbMembers(nb)                        => pong = pong + ("d" -> JsNumber(nb))
    case lila.hub.actorApi.round.NbRounds(nb) =>
      pong = pong + ("r" -> JsNumber(nb))

    case ChangeFeatured(_, msg) => notifyAllActiveAsync(msg)

    case SetIdle(uid, true)     => idleUids += uid
    case SetIdle(uid, false)    => idleUids -= uid
    case SocketLeave(uid, _)    => idleUids -= uid
  }

  private def notifyPlayerStart(game: lila.game.Game, color: chess.Color) =
    notifyMember("redirect", Json.obj(
      "id" -> (game fullIdOf color),
      "url" -> playerUrl(game fullIdOf color),
      "cookie" -> AnonCookie.json(game, color)
    ).noNull) _

  def notifyAllActiveAsync(msg: JsObject) = scala.concurrent.Future {
    members.foreach {
      case (uid, member) => if (!idleUids(uid)) member push msg
    }
  }

  override def sendMessage(message: Message)(member: Member) =
    if (!idleUids(member.uid)) member push {
      if (shouldSkipMessageFor(message, member)) message.skipMsg
      else message.fullMsg
    }

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    message.metadata.hook ?? { hook =>
      hook.uid != member.uid && !Biter.canJoin(hook, member.user)
    }

  private def playerUrl(fullId: String) = s"/$fullId"

  private def notifySeeks() {
    notifyAll(makeMessage("reload_seeks"))
  }
}
