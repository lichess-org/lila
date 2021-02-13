package lila.puzzle

import play.api.data._
import play.api.data.Forms._

object PuzzleForm {

  val round = Form(
    single(
      "win" -> number
    )
  )

  val vote = Form(
    single(
      "vote" -> number
    )
  )
}
