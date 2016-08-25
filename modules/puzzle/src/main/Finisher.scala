package lila.puzzle

import org.goochjs.glicko2._
import org.joda.time.DateTime

import lila.db.dsl._
import lila.rating.{ Glicko, Perf }
import lila.user.{ User, UserRepo }

private[puzzle] final class Finisher(
    api: PuzzleApi,
    puzzleColl: Coll) {

  private val maxTime = 5 * 60 * 1000

  def apply(puzzle: Puzzle, user: User, data: DataForm.RoundData): Fu[(Round, Option[Boolean])] =
    api.head.find(user) flatMap {
      case Some(PuzzleHead(_, Some(c), _)) if c == puzzle.id =>
        api.head.solved(user, puzzle.id)
        val userRating = user.perfs.puzzle.toRating
        val puzzleRating = puzzle.perf.toRating
        updateRatings(userRating, puzzleRating, data.isWin.fold(Glicko.Result.Win, Glicko.Result.Loss))
        val date = DateTime.now
        val puzzlePerf = puzzle.perf.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id} user")(puzzleRating, date)
        val userPerf = user.perfs.puzzle.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id}")(userRating, date)
        val a = new Round(
          id = Round.makeId(puzzle.id, user.id),
          puzzleId = puzzle.id,
          userId = user.id,
          date = DateTime.now,
          win = data.isWin,
          time = math.min(data.time, maxTime),
          userRating = user.perfs.puzzle.intRating,
          userRatingDiff = userPerf.intRating - user.perfs.puzzle.intRating)
        (api.learning.update(user, puzzle, data) >> (api.round add a) >> {
          puzzleColl.update(
            $id(puzzle.id),
            $inc(
              Puzzle.BSONFields.attempts -> $int(1),
              Puzzle.BSONFields.wins -> $int(data.isWin ? 1 | 0)
            ) ++ $set(
              Puzzle.BSONFields.perf -> Perf.perfBSONHandler.write(puzzlePerf)
            )) zip UserRepo.setPerf(user.id, "puzzle", userPerf)
        }) recover lila.db.recoverDuplicateKey(_ => ()) inject (a -> none)
      case _ => 
        val a = new Round(
          id = Round.makeId(puzzle.id, user.id),
          puzzleId = puzzle.id,
          userId = user.id,
          date = DateTime.now,
          win = data.isWin,
          time = math.min(data.time, maxTime),
          userRating = user.perfs.puzzle.intRating,
          userRatingDiff = 0)
          fuccess(a -> data.isWin.some)
    }

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d
  private val system = new RatingCalculator(VOLATILITY, TAU)

  private def mkRating(perf: Perf) = new Rating(
    math.max(1000, perf.glicko.rating),
    perf.glicko.deviation,
    perf.glicko.volatility, perf.nb)

  private def updateRatings(u1: Rating, u2: Rating, result: Glicko.Result) {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(u1, u2)
      case Glicko.Result.Win  => results.addResult(u1, u2)
      case Glicko.Result.Loss => results.addResult(u2, u1)
    }
    try {
      system.updateRatings(results)
    }
    catch {
      case e: Exception => logger.error("finisher", e)
    }
  }
}
