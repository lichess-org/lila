package lila.puzzle

import lila.db.AsyncColl
import lila.db.dsl._
import lila.user.User
import lila.memo.CacheApi._

final private[puzzle] class PuzzleBatch(
    puzzleColl: AsyncColl,
    api: PuzzleApi,
    finisher: Finisher,
    puzzleIdMin: PuzzleId
)(implicit ec: scala.concurrent.ExecutionContext) {

  def solve(originalUser: User, data: PuzzleBatch.SolveData): Funit =
    for {
      puzzles <- api.puzzle findMany data.solutions.map(_.id)
      user <- lila.common.Future.fold(data.solutions zip puzzles)(originalUser) {
        case (user, (solution, Some(puzzle))) => finisher.ratedUntrusted(puzzle, user, solution.result)
        case (user, _)                        => fuccess(user)
      }
      _ <- data.solutions.lastOption ?? { lastSolution =>
        api.head.solved(user, lastSolution.id).void
      }
    } yield for {
      first <- puzzles.headOption.flatten
      last  <- puzzles.lastOption.flatten
    } {
      if (puzzles.sizeIs > 1) logger.info(s"Batch solve ${user.id} ${puzzles.size} ${first.id}->${last.id}")
    }

  object select {

    import Selector._

    def apply(user: User, nb: Int, after: Option[PuzzleId]): Fu[List[Puzzle]] = {
      api.head.find(user) flatMap {
        newPuzzlesForUser(user, _, nb, after)
      } addEffect { puzzles =>
        lila.mon.puzzle.batch.selector.count.increment(puzzles.size)
      }
    }.mon(_.puzzle.batch.selector.time)

    private def newPuzzlesForUser(
        user: User,
        headOption: Option[PuzzleHead],
        nb: Int,
        after: Option[PuzzleId]
    ): Fu[List[Puzzle]] = {
      val rating = user.perfs.puzzle.intRating min 2300 max 900
      val step   = toleranceStepFor(rating, user.perfs.puzzle.nb)
      api.puzzle.cachedLastId.getUnit flatMap { maxId =>
        val fromId = headOption match {
          case Some(PuzzleHead(_, _, l)) if l < maxId - 500 => after.fold(l)(_ atLeast l)
          case _                                            => puzzleIdMin
        }
        puzzleColl { coll =>
          tryRange(
            coll = coll,
            rating = rating,
            tolerance = step,
            step = step,
            idRange = Range(fromId, fromId + nb * 50),
            nb = nb
          )
        }
      }
    }

    private def tryRange(
        coll: Coll,
        rating: Int,
        tolerance: Int,
        step: Int,
        idRange: Range,
        nb: Int
    ): Fu[List[Puzzle]] =
      coll
        .find(
          rangeSelector(
            rating = rating,
            tolerance = tolerance,
            idRange = idRange
          )
        )
        .cursor[Puzzle]()
        .list(nb) flatMap {
        case res if res.sizeIs < nb && (tolerance + step) <= toleranceMax =>
          tryRange(
            coll = coll,
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
  implicit private val SolutionReads = Json.reads[Solution]
  implicit val SolveDataReads        = Json.reads[SolveData]
}
