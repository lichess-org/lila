package lila
package lobby

import socket._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(
    messenger: Messenger,
    val history: History,
    timeout: Int) extends HubActor[Member](timeout) with Historical[Member] {

  def receiveSpecific = {

    case PingVersion(uid, v) ⇒ {
      ping(uid)
      withMember(uid) { m ⇒
        history.since(v).fold(_ foreach m.channel.push, resync(m))
      }
    }

    case WithHooks(op) ⇒ op(hookOwnerIds).unsafePerformIO

    case Join(uid, username, version, hookOwnerId) ⇒ {
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      addMember(uid, Member(channel, username, hookOwnerId))
      sender ! Connected(enumerator, channel)
    }

    case Talk(u, txt) ⇒ messenger(u, txt).unsafePerformIO foreach { message ⇒
      notifyVersion("talk", Seq(
        "u" -> JsString(message.username),
        "txt" -> JsString(message.text)
      ))
    }

    case SysTalk(txt) ⇒ messenger.system(txt).unsafePerformIO |> { message ⇒
      notifyVersion("talk", Seq("txt" -> JsString(message.text)))
    }

    case ReloadTournaments(html) => notifyTournaments(html)

    case AddEntry(entry) ⇒ notifyVersion("entry", JsString(entry.render))

    case AddHook(hook) ⇒ notifyVersion("hook_add", Seq(
      "id" -> JsString(hook.id),
      "username" -> JsString(hook.username),
      "elo" -> hook.elo.fold(JsNumber(_), JsNull),
      "mode" -> JsString(hook.realMode.toString),
      "variant" -> JsString(hook.realVariant.toString),
      "color" -> JsString(hook.color),
      "clock" -> JsString(hook.clockOrUnlimited),
      "emin" -> hook.realEloRange.fold(range ⇒ JsNumber(range.min), JsNull),
      "emax" -> hook.realEloRange.fold(range ⇒ JsNumber(range.max), JsNull),
      "engine" -> JsBoolean(hook.engine))
    )

    case RemoveHook(hook) ⇒ notifyVersion("hook_remove", JsString(hook.id))

    case BiteHook(hook, game) ⇒ notifyMember(
      "redirect", JsString(game fullIdOf game.creatorColor)
    ) _ |> { fn ⇒
        members.values filter (_ ownsHook hook) foreach fn
      }

    case ChangeFeatured(html) ⇒ notifyFeatured(html)
  }

  def notifyFeatured(html: String) {
    val msg = makeMessage("featured", JsString(html))
    members.values foreach (_.channel push msg)
  }

  def notifyTournaments(html: String) {
    val msg = makeMessage("tournaments", JsString(html))
    members.values foreach (_.channel push msg)
  }

  def hookOwnerIds: Iterable[String] =
    members.values.map(_.hookOwnerId).flatten
}
