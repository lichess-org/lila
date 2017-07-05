package lila.puzzle

import org.goochjs.glicko2._
import org.joda.time.DateTime

import chess.Mode
import lila.db.dsl._
import lila.rating.{ Glicko, PerfType }
import lila.user.{ User, UserRepo }

private[puzzle] final class Finisher(
    api: PuzzleApi,
    puzzleColl: Coll
) {

  def apply(puzzle: Puzzle, user: User, result: Result): Fu[(Round, Mode)] =
    api.head.find(user) flatMap {
      case Some(PuzzleHead(_, Some(c), _)) if c == puzzle.id =>
        api.head.solved(user, puzzle.id) >>
          api.learning.update(user, puzzle, result).flatMap { isLearning =>
            val date = DateTime.now
            val userRating = user.perfs.puzzle.toRating(date)
            val puzzleRating = puzzle.perf.toRating(date)
            updateRatings(userRating, puzzleRating,
              result = result.win.fold(Glicko.Result.Win, Glicko.Result.Loss),
              isLearning = isLearning)
            val puzzlePerf = puzzle.perf.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id} user")(puzzleRating, date)
            val userPerf = user.perfs.puzzle.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id}")(userRating, date)
            val a = new Round(
              puzzleId = puzzle.id,
              userId = user.id,
              date = date,
              result = result,
              rating = user.perfs.puzzle.intRating,
              ratingDiff = userPerf.intRating - user.perfs.puzzle.intRating
            )
            (api.round add a) >> {
              puzzleColl.update(
                $id(puzzle.id),
                $inc(Puzzle.BSONFields.attempts -> $int(1)) ++
                  $set(Puzzle.BSONFields.perf -> PuzzlePerf.puzzlePerfBSONHandler.write(puzzlePerf))
              ) zip UserRepo.setPerf(user.id, PerfType.Puzzle, userPerf)
            } inject (a -> Mode.Rated)
          }
      case _ =>
        incPuzzleAttempts(puzzle)
        val a = new Round(
          puzzleId = puzzle.id,
          userId = user.id,
          date = DateTime.now,
          result = result,
          rating = user.perfs.puzzle.intRating,
          ratingDiff = 0
        )
        fuccess(a -> Mode.Casual)
    }

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d
  private val system = new RatingCalculator(VOLATILITY, TAU)

  def incPuzzleAttempts(puzzle: Puzzle) =
    puzzleColl.incFieldUnchecked($id(puzzle.id), Puzzle.BSONFields.attempts)

  private def updateRatings(u1: Rating, u2: Rating, result: Glicko.Result, isLearning: Boolean) {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(u1, u2)
      case Glicko.Result.Win => results.addResult(u1, u2)
      case Glicko.Result.Loss => results.addResult(u2, u1)
    }
    try {
      val (r1, r2) = (u1.getRating, u2.getRating)
      system.updateRatings(results)
      if (isLearning) {
        def mitigate(prev: Double, next: Rating) = next.setRating((next.getRating + prev) / 2)
        mitigate(r1, u1)
        mitigate(r2, u2)
      }
      // never take away more than 30 rating points - it just causes upsets
      List(r1 -> u1, r2 -> u2).foreach {
        case (prev, next) if next.getRating - prev < -30 => next.setRating(prev - 30)
        case _ =>
      }
    }
    catch {
      case e: Exception => logger.error("finisher", e)
    }
  }
}
