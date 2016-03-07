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
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }
import makeTimeout.short

private[lobby] final class Socket(
    val history: History[Messadata],
    router: akka.actor.ActorSelection,
    uidTtl: FiniteDuration) extends SocketActor[Member](uidTtl) with Historical[Member, Messadata] {

  override val startsOnApplicationBoot = true

  override def preStart {
    super.preStart
    context.system.lilaBus.subscribe(self, 'changeFeaturedGame, 'streams, 'nbMembers)
  }

  def receiveSpecific = {

    case PingVersion(uid, v) =>
      ping(uid)
      withMember(uid) { m =>
        history.since(v).fold(resync(m))(_ foreach sendMessage(m))
      }

    case Join(uid, user, blocks, mobile) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, blocks, uid, mobile)
      addMember(uid, member)
      sender ! Connected(enumerator, member)

    case ReloadTournaments(html) => notifyAllAsync(makeMessage("tournaments", html))

    case ReloadSimuls(html)      => notifyAllAsync(makeMessage("simuls", html))

    case NewForumPost            => notifyAllAsync("reload_forum")

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
      notifyMobileUsers(makeMessage("nbr", nb)) // BC, remove me

    case ChangeFeatured(_, msg) => notifyAllAsync(msg)
  }

  private def notifyPlayerStart(game: lila.game.Game, color: chess.Color) =
    notifyMember("redirect", Json.obj(
      "id" -> (game fullIdOf color),
      "url" -> playerUrl(game fullIdOf color),
      "cookie" -> AnonCookie.json(game, color)
    ).noNull) _

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    message.metadata.hook ?? { hook =>
      hook.uid != member.uid && !Biter.canJoin(hook, member.user)
    }

  private def playerUrl(fullId: String) = s"/$fullId"

  private def notifySeeks() {
    notifyAll(makeMessage("reload_seeks"))
  }

  def notifyMobileUsers(msg: JsObject) = Future {
    members.values.foreach { m =>
      if (m.mobile) m push msg
    }
  }
}
