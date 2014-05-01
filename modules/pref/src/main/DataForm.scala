package lila.pref

import play.api.data._
import play.api.data.Forms._

import lila.user.User

private[pref] final class DataForm(api: PrefApi) {

  val pref = Form(mapping(
    "autoQueen" -> number.verifying(Pref.AutoQueen.choices.toMap contains _),
    "autoThreefold" -> number.verifying(Pref.AutoThreefold.choices.toMap contains _),
    "takeback" -> number.verifying(Pref.Takeback.choices.toMap contains _),
    "clockTenths" -> number.verifying(Set(0, 1) contains _),
    "clockBar" -> number.verifying(Set(0, 1) contains _),
    "premove" -> number.verifying(Set(0, 1) contains _)
  )(PrefData.apply)(PrefData.unapply))

  case class PrefData(
      autoQueen: Int,
      autoThreefold: Int,
      takeback: Int,
      clockTenths: Int,
      clockBar: Int,
      premove: Int) {

    def apply(pref: Pref) = pref.copy(
      autoQueen = autoQueen,
      autoThreefold = autoThreefold,
      takeback = takeback,
      clockTenths = clockTenths == 1,
      clockBar = clockBar == 1,
      premove = premove == 1)
  }

  object PrefData {

    def apply(pref: Pref): PrefData = PrefData(
      autoQueen = pref.autoQueen,
      autoThreefold = pref.autoThreefold,
      takeback = pref.takeback,
      clockTenths = pref.clockTenths.fold(1, 0),
      clockBar = pref.clockBar.fold(1, 0),
      premove = pref.premove.fold(1, 0))
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
