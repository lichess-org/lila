package lidraughts.puzzle

import draughts.variant.Variant
import lidraughts.db.dsl._
import lidraughts.user.User
import Puzzle.{ BSONFields => F }

private[puzzle] final class PuzzleBatch(
    puzzleColl: Map[Variant, Coll],
    api: PuzzleApi,
    finisher: Finisher,
    puzzleIdMin: Int
) {

  def solve(originalUser: User, variant: Variant, data: PuzzleBatch.SolveData): Funit = for {
    puzzles <- api.puzzle.findMany(data.solutions.map(_.id), variant)
    user <- lidraughts.common.Future.fold(data.solutions zip puzzles)(originalUser) {
      case (user, (solution, Some(puzzle))) => finisher.ratedUntrusted(puzzle, user, solution.result)
      case (user, _) => fuccess(user)
    }
    _ <- data.solutions.lastOption ?? { lastSolution =>
      api.head.solved(user, lastSolution.id, variant).void
    }
  } yield for {
    first <- puzzles.headOption.flatten
    last <- puzzles.lastOption.flatten
  } {
    if (puzzles.size > 1) logger.info(s"Batch solve ${user.id} ${puzzles.size} ${first.id}->${last.id}")
  }

  object select {

    import Selector._

    def apply(user: User, variant: Variant, nb: Int): Fu[List[Puzzle]] = {
      api.head.find(user, variant) flatMap {
        newPuzzlesForUser(user, variant, _, nb)
      } flatMap { puzzles =>
        lidraughts.mon.puzzle.batch.selector.count(puzzles.size)
        puzzles.lastOption.?? { p => api.head.addNew(user, p.id, variant) } inject puzzles
      }
    }.mon(_.puzzle.batch.selector.time)

    private def newPuzzlesForUser(user: User, variant: Variant, headOption: Option[PuzzleHead], nb: Int): Fu[List[Puzzle]] = {
      val perf = user.perfs.puzzle(variant)
      val rating = perf.intRating min 2300 max 900
      val step = toleranceStepFor(rating, perf.nb)
      api.puzzle.cachedLastId(variant).get flatMap { maxId =>
        val lastId = headOption match {
          case Some(PuzzleHead(_, _, l)) if l < maxId - 500 => l
          case _ => puzzleIdMin
        }
        tryRange(
          variant = variant,
          rating = rating,
          tolerance = step,
          step = step,
          idRange = Range(lastId, lastId + nb * 50),
          nb = nb
        )
      }
    }

    private def tryRange(
      variant: Variant,
      rating: Int,
      tolerance: Int,
      step: Int,
      idRange: Range,
      nb: Int
    ): Fu[List[Puzzle]] = puzzleColl(variant).find(rangeSelector(
      rating = rating,
      tolerance = tolerance,
      idRange = idRange
    )).list[Puzzle](nb) flatMap {
      case res if res.size < nb && (tolerance + step) <= toleranceMax =>
        tryRange(
          variant = variant,
          rating = rating,
          tolerance = tolerance + step,
          step = step,
          idRange = Range(idRange.min, idRange.max + 100),
          nb = nb
        )
      case res => fuccess(res)
    }
  }
}

object PuzzleBatch {

  case class Solution(id: PuzzleId, win: Boolean) {
    def result = Result(win)
  }
  case class SolveData(solutions: List[Solution])

  import play.api.libs.json._
  private implicit val SolutionReads = Json.reads[Solution]
  implicit val SolveDataReads = Json.reads[SolveData]
}
