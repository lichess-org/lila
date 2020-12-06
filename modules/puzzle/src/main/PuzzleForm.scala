package lila.puzzle

import play.api.data._
import play.api.data.Forms._

import lila.common.Form.stringIn

object PuzzleForm {

  val round = Form(
    single("win" -> number)
  )

  val vote = Form(
    single("vote" -> boolean)
  )

  val themeVote = Form(
    single("vote" -> optional(boolean))
  )

  val difficulty = Form(
    single("difficulty" -> stringIn(PuzzleDifficulty.all.map(_.key).toSet))
  )
}
