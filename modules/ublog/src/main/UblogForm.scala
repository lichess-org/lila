package lila.ublog

import play.api.data._
import play.api.data.Forms._
import lila.common.Form.{ cleanNonEmptyText, cleanText }

final class UblogForm {

  import UblogForm._

  val form = Form(
    mapping(
      "title"    -> cleanNonEmptyText(minLength = 3, maxLength = 100),
      "intro"    -> cleanNonEmptyText(minLength = 0, maxLength = 2_000),
      "markdown" -> cleanNonEmptyText(minLength = 0, maxLength = 100_000)
    )(UblogPostData.apply)(UblogPostData.unapply)
  )
}

object UblogForm {

  case class UblogPostData(title: String, intro: String, markdown: String)
}
