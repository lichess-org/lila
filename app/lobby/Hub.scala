package lila
package lobby

import db.MessageRepo

import akka.actor._

import play.api.libs.json._
import play.api.libs.iteratee._

final class Hub(messageRepo: MessageRepo, history: History) extends Actor {

  private var members = Map.empty[String, Member]

  def receive = {

    case GetCount ⇒ sender ! members.size

    case GetHooks ⇒ sender ! Hooks(members.values collect {
      case Member(_, Some(hook)) ⇒ hook
    })

    case Join(uid, version, hookOwnerId) ⇒ {
      val channel = new LilaEnumerator[JsValue](history since version)
      members = members + (uid -> Member(channel, hookOwnerId))
      sender ! Connected(channel)
    }

    case Talk(txt, u) ⇒ messageRepo.add(txt, u).foreach { save ⇒
      val message = save.unsafePerformIO
      notifyVersion("talk", Seq(
        "txt" -> JsString(message.text),
        "u" -> JsString(message.username)
      ))
    }

    case Entry(entry) ⇒ notifyVersion("entry", JsString(entry.render))

    case AddHook(hook) ⇒ notifyVersion("hook_add", Seq(
      "id" -> JsString(hook.id),
      "username" -> JsString(hook.username),
      "elo" -> hook.elo.fold(JsNumber(_), JsNull),
      "mode" -> JsString(hook.realMode.toString),
      "variant" -> JsString(hook.realVariant.toString),
      "color" -> JsString(hook.color),
      "clock" -> JsString(hook.clockOrUnlimited),
      "emin" -> hook.eloMin.fold(JsNumber(_), JsNull),
      "emax" -> hook.eloMax.fold(JsNumber(_), JsNull),
      "engine" -> JsBoolean(hook.engine))
    )

    case RemoveHook(hook) ⇒ notifyVersion("hook_remove", JsString(hook.id))

    case BiteHook(hook, game) ⇒ notifyMember(
      "redirect", JsString(game fullIdOf game.creatorColor)
    ) _ |> { fn ⇒
        members.values filter (_ ownsHook hook) foreach fn
      }

    case NbPlayers(nb) ⇒ notifyAll("nbp", JsNumber(nb))

    case Quit(uid)     ⇒ { members = members - uid }
  }

  def notifyMember(t: String, data: JsValue)(member: Member) {
    val msg = JsObject(Seq("t" -> JsString(t), "d" -> data))
    member.channel push msg
  }

  def notifyAll(t: String, data: JsValue) {
    val msg = makeMessage(t, data)
    members.values.foreach(_.channel push msg)
  }

  def notifyVersion(t: String, data: JsValue) {
    val vmsg = history += makeMessage(t, data)
    members.values.foreach(_.channel push vmsg)
  }
  def notifyVersion(t: String, data: Seq[(String, JsValue)]) {
    notifyVersion(t, JsObject(data))
  }

  private def makeMessage(t: String, data: JsValue) =
    JsObject(Seq("t" -> JsString(t), "d" -> data))
}
