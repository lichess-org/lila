package lila.lobby

import actorApi._
import lila.socket.{ SocketActor, History, Historical }
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.game.actorApi._
import lila.hub.actorApi.lobby._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.templates.Html
import scala.concurrent.duration._

private[lobby] final class Socket(
  messenger: Messenger,
  val history: History,
  uidTtl: Duration)
    extends SocketActor[Member](uidTtl) with Historical[Member] {

  def receiveSpecific = {

    case PingVersion(uid, v) ⇒ {
      ping(uid)
      withMember(uid) { m ⇒
        history.since(v).fold(resync(m))(_ foreach m.channel.push)
      }
    }

    case WithHooks(op) ⇒ op(hookOwnerIds)

    case Join(uid, user, hookOwnerId) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user, hookOwnerId)
      addMember(uid, member)
      sender ! Connected(enumerator, member)
    }

    case Message(user, text, troll, _) ⇒ notifyVersion("talk", Json.obj(
      "u" -> user,
      "t" -> text,
      "x" -> troll
    ))

    case SysTalk(txt) ⇒ messenger system txt foreach { message ⇒
      notifyVersion("talk", Json.obj("t" -> message.text))
    }

    case UnTalk(regex)           ⇒ notifyVersion("untalk", Json.obj(
      "regex" -> regex.toString
    ))

    case ReloadTournaments(html) ⇒ notifyTournaments(html)

    case TimelineEntry(rendered) ⇒ notifyVersion("entry", rendered)

    case Censor(userId)          ⇒ // TODO hide user messages right away?

    case AddHook(hook) ⇒ notifyVersion("hook_add", Json.obj(
      "id" -> hook.id,
      "username" -> hook.username,
      "elo" -> hook.elo,
      "mode" -> hook.realMode.toString,
      "variant" -> hook.realVariant.toString,
      "color" -> hook.color,
      "clock" -> hook.clockOrUnlimited,
      "emin" -> hook.realEloRange.map(_.min),
      "emax" -> hook.realEloRange.map(_.max),
      "engine" -> hook.engine))

    case RemoveHook(hook) ⇒ notifyVersion("hook_remove", hook.id)

    case BiteHook(hook, game) ⇒ notifyMember(
      "redirect", game fullIdOf game.creatorColor) _ |> { fn ⇒
        members.values filter (_ ownsHook hook) foreach fn
      }

    case ChangeFeatured(html) ⇒ notifyFeatured(html)
  }

  private def notifyFeatured(html: Html) {
    broadcast(makeMessage("featured", html.toString))
  }

  private def notifyTournaments(html: String) {
    broadcast(makeMessage("tournaments", html))
  }

  private def broadcast(msg: JsObject) {
    members.values foreach (_.channel push msg)
  }

  private def hookOwnerIds: Iterable[String] =
    members.values.map(_.hookOwnerId).flatten
}
