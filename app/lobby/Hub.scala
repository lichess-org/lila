package lila
package lobby

import socket._

import akka.actor._
import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(
    messenger: Messenger,
    history: History,
    timeout: Int) extends HubActor[Member](timeout) {

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

    case ChangeFeatured(oldId, newId) ⇒ notifyFeatured(oldId, newId)
  }

  def notifyMember(t: String, data: JsValue)(member: Member) {
    val msg = JsObject(Seq("t" -> JsString(t), "d" -> data))
    member.channel push msg
  }

  def notifyVersion(t: String, data: JsValue) {
    val vmsg = history += makeMessage(t, data)
    members.values.foreach(_.channel push vmsg)
  }
  def notifyVersion(t: String, data: Seq[(String, JsValue)]) {
    notifyVersion(t, JsObject(data))
  }

  def notifyFeatured(oldId: Option[String], newId: String) {
    val msg = makeMessage("featured", JsObject(Seq(
      "oldId" -> oldId.fold(JsString(_), JsNull),
      "newId" -> JsString(newId))))
    members.values foreach (_.channel push msg)
  }

  def hookOwnerIds: Iterable[String] =
    members.values.map(_.hookOwnerId).flatten
}
