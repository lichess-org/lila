package lila.puzzle

import play.api.data._
import play.api.data.Forms._

object DataForm {

  val round = Form(single(
    "win" -> number
  ))

  val vote = Form(single(
    "vote" -> number
  ))
}
