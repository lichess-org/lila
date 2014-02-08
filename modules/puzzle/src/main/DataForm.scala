package lila.puzzle

import play.api.data._
import play.api.data.Forms._

object DataForm {

  val attempt = Form(mapping(
    "win" -> number,
    "time" -> number
  )(AttemptData.apply)(AttemptData.unapply))

  case class AttemptData(
      win: Int,
      time: Int) {

    def isWin = win == 1
  }

  val vote = Form(single(
    "vote" -> number
  ))
}
