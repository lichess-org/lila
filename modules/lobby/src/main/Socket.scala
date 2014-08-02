package lila.lobby

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import play.api.libs.iteratee._
import play.api.libs.json._
import play.twirl.api.Html

import actorApi._
import lila.common.PimpedJson._
import lila.game.actorApi._
import lila.game.AnonCookie
import lila.hub.actorApi.lobby._
import lila.hub.actorApi.router.{ Homepage, Player }
import lila.hub.actorApi.timeline._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.{ SocketActor, History, Historical }
import makeTimeout.short

private[lobby] final class Socket(
    val history: History[Messadata.type],
    router: akka.actor.ActorSelection,
    uidTtl: Duration) extends SocketActor[Member](uidTtl) with Historical[Member, Messadata.type] {

  context.system.lilaBus.subscribe(self, 'changeFeaturedGame, 'streams)

  def receiveSpecific = {

    case PingVersion(uid, v) =>
      ping(uid)
      withMember(uid) { m =>
        history.since(v).fold(resync(m))(_ foreach sendMessage(m))
      }

    case Join(uid, user) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      sender ! Connected(enumerator, member)

    case ReloadTournaments(html) => notifyTournaments(html)

    case NewForumPost            => notifyAll("reload_forum")

    case ReloadTimeline(user)    => sendTo(user, makeMessage("reload_timeline", JsNull))

    case AddHook(hook)           => notifyVersion("hook_add", hook.render, Messadata)

    case RemoveHook(hookId)      => notifyVersion("hook_remove", hookId, Messadata)

    case JoinHook(uid, hook, game, creatorColor) =>
      withMember(hook.uid)(notifyMember("redirect", Json.obj(
        "id" -> (game fullIdOf creatorColor),
        "url" -> playerUrl(game fullIdOf creatorColor),
        "cookie" -> AnonCookie.json(game, creatorColor)
      ).noNull))
      withMember(uid)(notifyMember("redirect", Json.obj(
        "id" -> (game fullIdOf !creatorColor),
        "url" -> playerUrl(game fullIdOf !creatorColor),
        "cookie" -> AnonCookie.json(game, !creatorColor)
      ).noNull))

    case HookIds(ids)                         => notifyVersion("hook_list", ids, Messadata)

    case lila.hub.actorApi.StreamsOnAir(html) => notifyAll(makeMessage("streams", html))
  }

  protected def shouldSkipMessageFor(message: Message, member: Member) = false

  private def playerUrl(fullId: String) = s"/$fullId"

  private def notifyTournaments(html: String) {
    notifyAll(makeMessage("tournaments", html))
  }
}
