package lila.pref

import play.api.data._
import play.api.data.Forms._

object DataForm {

  val theme = Form(single(
    "theme" -> nonEmptyText.verifying(Theme contains _)
  ))

  val bg = Form(single(
    "bg" -> text.verifying(Set("light", "dark") contains _)
  ))
}
