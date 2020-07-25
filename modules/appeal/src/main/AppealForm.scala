package lila.appeal

import play.api.data._
import play.api.data.Forms._

final class AppealForm {

  val text = Form(
    single("text" -> nonEmptyText)
  )
}
