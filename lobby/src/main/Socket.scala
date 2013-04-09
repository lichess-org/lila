package lila.lobby

import actorApi._
import lila.socket.{ SocketActor, History, Historical }
import lila.socket.actorApi._
import lila.game.actorApi._
import lila.hub.actorApi.lobby._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._
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

    case Join(uid, userId, version, hookOwnerId) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, userId, hookOwnerId)
      addMember(uid, member)
      sender ! Connected(enumerator, member)
    }

    case Talk(u, txt) ⇒ messenger(u, txt) foreach {
      _.fold(
        err ⇒ logwarn(err.shows),
        message ⇒ notifyVersion("talk", Json.obj(
          "u" -> message.userId,
          "txt" -> message.text
        ))
      )
    }

    case SysTalk(txt) ⇒ messenger system txt foreach { message ⇒
      notifyVersion("talk", Json.obj("txt" -> message.text))
    }

    case UnTalk(regex) ⇒ (messenger remove regex) >>
      notifyVersion("untalk", Json.obj("regex" -> regex.toString))

    case ReloadTournaments(html) ⇒ notifyTournaments(html)

    case TimelineEntry(rendered) ⇒ notifyVersion("entry", rendered)

    case Censor(userId) => // TODO hide user messages right away?

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

  def notifyFeatured(html: Html) {
    val msg = makeMessage("featured", html.toString)
    members.values foreach (_.channel push msg)
  }

  def notifyTournaments(html: String) {
    val msg = makeMessage("tournaments", html)
    members.values foreach (_.channel push msg)
  }

  def hookOwnerIds: Iterable[String] =
    members.values.map(_.hookOwnerId).flatten
}
