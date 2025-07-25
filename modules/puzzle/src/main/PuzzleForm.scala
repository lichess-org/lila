package lila.puzzle

import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.*
import chess.Rated

import lila.common.Form.{ into, numberIn, stringIn, typeIn, given }
import lila.common.Json.given
import scalalib.model.Days

object PuzzleForm:

  val maxStreakScore = 250

  case class RoundData(
      win: PuzzleWin,
      rated: Rated,
      replayDays: Option[Days],
      streakId: Option[String],
      streakScore: Option[Int],
      color: Option[Color]
  ):
    def streakPuzzleId = streakId.flatMap(Puzzle.toId)

  val round = Form(
    mapping(
      "win" -> of[PuzzleWin],
      "rated" -> boolean.into[Rated],
      "replayDays" -> optional(typeIn[Days](PuzzleDashboard.dayChoices.toSet)),
      "streakId" -> optional(nonEmptyText),
      "streakScore" -> optional(number(min = 0, max = maxStreakScore)),
      "color" -> optional(lila.common.Form.color.mapping)
    )(RoundData.apply)(unapply)
  )

  val vote = Form(
    single("vote" -> boolean)
  )

  val report = Form(
    single("reason" -> nonEmptyText(1, 2000))
  )

  val themeVote = Form(
    single("vote" -> optional(boolean))
  )

  val difficulty = Form(
    single("difficulty" -> stringIn(PuzzleDifficulty.all.map(_.key).toSet))
  )

  object batch:
    case class Solution(id: PuzzleId, win: PuzzleWin, rated: Rated = Rated.Yes)
    case class SolveData(solutions: List[Solution])
    given Reads[Solution] = Json.reads
    given Reads[SolveData] = Json.reads

  object bc:

    val round = Form(
      mapping(
        "win" -> text
      )(w => RoundData(win = PuzzleWin(w == "1" || w == "true"), rated = Rated.Yes, none, none, none, none))(
        _ => none
      )
    )

    val vote = Form(single("vote" -> numberIn(Set(0, 1))))

    case class SolutionBc(id: Long, win: PuzzleWin)
    case class SolveDataBc(solutions: List[SolutionBc])
    given Reads[SolutionBc] = Json.reads
    given Reads[SolveDataBc] = Json.reads
