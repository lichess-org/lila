package lila.puzzle

import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._

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

  object bc {

    val round = Form(
      mapping(
        "win" -> text
      )(w => RoundData(w == "1" || w == "true", none))(_ => none)
    )

    val vote = Form(
      single("vote" -> numberIn(Set(0, 1)))
    )

    case class Solution(id: Long, win: Boolean)
    case class SolveData(solutions: List[Solution])

    implicit val SolutionReads  = Json.reads[Solution]
    implicit val SolveDataReads = Json.reads[SolveData]
  }
}
