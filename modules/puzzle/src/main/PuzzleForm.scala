package lila.puzzle

import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.*

import lila.common.Form.{ numberIn, stringIn, given }
import lila.common.Json.given
import chess.Color

object PuzzleForm:

  val maxStreakScore = 250

  case class RoundData(
      win: PuzzleWin,
      rated: Boolean,
      replayDays: Option[Int],
      streakId: Option[String],
      streakScore: Option[Int],
      color: Option[Color]
  ):
    def streakPuzzleId = streakId flatMap Puzzle.toId
    def mode           = chess.Mode(rated)

  val round = Form(
    mapping(
      "win"         -> of[PuzzleWin],
      "rated"       -> boolean,
      "replayDays"  -> optional(numberIn(PuzzleDashboard.dayChoices)),
      "streakId"    -> optional(nonEmptyText),
      "streakScore" -> optional(number(min = 0, max = maxStreakScore)),
      "color"       -> optional(lila.common.Form.color.mapping)
    )(RoundData.apply)(unapply)
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

  object batch:
    case class Solution(id: PuzzleId, win: PuzzleWin, rated: Boolean = true):
      def mode = chess.Mode(rated)
    case class SolveData(solutions: List[Solution])
    given Reads[Solution]  = Json.reads
    given Reads[SolveData] = Json.reads

  object bc:

    val round = Form(
      mapping(
        "win" -> text
      )(w => RoundData(win = PuzzleWin(w == "1" || w == "true"), rated = true, none, none, none, none))(_ =>
        none
      )
    )

    val vote = Form(single("vote" -> numberIn(Set(0, 1))))

    case class SolutionBc(id: Long, win: Boolean)
    case class SolveDataBc(solutions: List[SolutionBc])
    given Reads[SolutionBc]  = Json.reads
    given Reads[SolveDataBc] = Json.reads
