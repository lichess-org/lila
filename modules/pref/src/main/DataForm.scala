package lila.pref

import play.api.data._
import play.api.data.Forms._

import lila.user.User

private[pref] final class DataForm(api: PrefApi) {

  val pref = Form(mapping(
    "chat" -> boolean,
    "sound" -> boolean
  )(PrefData.apply)(PrefData.unapply))

  case class PrefData(
      chat: Boolean,
      sound: Boolean) {

    def apply(pref: Pref) = pref.copy(
      chat = chat,
      sound = sound)
  }

  object PrefData {

    def apply(pref: Pref): PrefData = PrefData(
      chat = pref.chat,
      sound = pref.sound)
  }

  def prefOf(user: User): Fu[Form[PrefData]] = api getPref user map { p ⇒
    pref fill PrefData(p)
  }

  val theme = Form(single(
    "theme" -> nonEmptyText.verifying(Theme contains _)
  ))

  val bg = Form(single(
    "bg" -> text.verifying(List("light", "dark") contains _)
  ))
}
