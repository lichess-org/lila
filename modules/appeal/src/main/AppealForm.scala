package lila.appeal

import play.api.data.Forms._
import play.api.data._

final class AppealForm {

  val text = Form(
    single("text" -> nonEmptyText),
  )
}
