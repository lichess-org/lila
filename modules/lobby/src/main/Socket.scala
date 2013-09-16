package lila.lobby

import scala.concurrent.duration._

import actorApi._
import akka.actor._
import akka.pattern.ask
import makeTimeout.short
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.templates.Html

import lila.game.actorApi._
import lila.hub.actorApi.lobby._
import lila.hub.actorApi.router.{ Homepage, Player }
import lila.hub.actorApi.timeline._
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.socket.{ SocketActor, History, Historical }

private[lobby] final class Socket(
    val history: History,
    router: lila.hub.ActorLazyRef,
    uidTtl: Duration) extends SocketActor[Member](uidTtl) with Historical[Member] {

  def receiveSpecific = {

    case PingVersion(uid, v) ⇒ {
      ping(uid)
      withMember(uid) { m ⇒
        history.since(v).fold(resync(m))(_ foreach m.channel.push)
      }
    }

    case Join(uid, user) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      sender ! Connected(enumerator, member)
    }

    case ReloadTournaments(html) ⇒ notifyTournaments(html)

    case GameEntryView(rendered) ⇒ notifyVersion("game_entry", rendered)

    case ReloadTimeline(user)    ⇒ sendTo(user, makeMessage("reload_timeline", JsNull))

    case AddHook(hook, _)        ⇒ notifyVersion("hook_add", hook.render)

    case RemoveHook(hookId)      ⇒ notifyVersion("hook_remove", hookId)

    case JoinHook(uid, hook, game) ⇒
      playerUrl(game fullIdOf game.creatorColor) zip
        playerUrl(game fullIdOf game.invitedColor) foreach {
          case (creatorUrl, invitedUrl) ⇒ {
            withMember(hook.uid)(notifyMember("redirect", creatorUrl))
            withMember(uid)(notifyMember("redirect", invitedUrl))
          }
        }

    case ChangeFeatured(html) ⇒ notifyFeatured(html)

    case HookIds(ids)         ⇒ notifyVersion("hook_list", ids)
  }

  private def playerUrl(fullId: String) =
    router ? Player(fullId) mapTo manifest[String]

  private def notifyFeatured(html: Html) {
    broadcast(makeMessage("featured", html.toString))
  }

  private def notifyTournaments(html: String) {
    broadcast(makeMessage("tournaments", html))
  }
}
