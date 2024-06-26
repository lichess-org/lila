package lila.puzzle

import play.api.data._
import play.api.data.Forms._

import lila.common.Form.{ numberIn, stringIn }

object PuzzleForm {

  case class RoundData(win: Boolean, replayDays: Option[Int]) {
    def result = Result(win)
  }

  val round = Form(
    mapping(
      "win"        -> boolean,
      "replayDays" -> optional(numberIn(PuzzleDashboard.dayChoices))
    )(RoundData.apply)(RoundData.unapply)
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

  val newPuzzles = Form(
    mapping(
      "sfens"  -> nonEmptyText,
      "source" -> optional(text)
    )(Tuple2.apply)(Tuple2.unapply)
  )

  object mobile {

    case class Solution(id: String, theme: String, win: Boolean)

    val solutions = Form(
      single(
        "solutions" -> list(
          mapping(
            "id"    -> nonEmptyText,
            "theme" -> nonEmptyText,
            "win"   -> boolean
          )(Solution.apply)(Solution.unapply)
        )
      )
    )

  }
}
