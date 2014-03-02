package lila.pref

import play.api.data._
import play.api.data.Forms._

import lila.user.User

private[pref] final class DataForm(api: PrefApi) {

  val pref = Form(mapping(
    "autoQueen" -> number.verifying(Pref.AutoQueen.choices.toMap contains _),
    "clockTenths" -> optional(number),
    "premove" -> optional(number)
  )(PrefData.apply)(PrefData.unapply))

  case class PrefData(
      autoQueen: Int,
      clockTenths: Option[Int],
      premove: Option[Int]) {

    def apply(pref: Pref) = pref.copy(
      autoQueen = autoQueen,
      clockTenths = clockTenths.isDefined,
      premove = premove.isDefined)
  }

  object PrefData {

    def apply(pref: Pref): PrefData = PrefData(
      autoQueen = pref.autoQueen,
      clockTenths = pref.clockTenths option 1,
      premove = pref.premove option 1)
  }

  def prefOf(user: User): Fu[Form[PrefData]] = api getPref user map { p =>
    pref fill PrefData(p)
  }

  val theme = Form(single(
    "theme" -> nonEmptyText.verifying(Theme contains _)
  ))

  val bg = Form(single(
    "bg" -> text.verifying(List("light", "dark") contains _)
  ))
}
