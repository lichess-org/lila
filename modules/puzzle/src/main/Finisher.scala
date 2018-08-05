package lila.puzzle

import org.goochjs.glicko2._
import org.joda.time.DateTime

import chess.Mode
import lila.db.dsl._
import lila.rating.{ Glicko, PerfType }
import lila.user.{ User, UserRepo }

import reactivemongo.api.WriteConcern

private[puzzle] final class Finisher(
    api: PuzzleApi,
    puzzleColl: Coll,
    bus: lila.common.Bus
) {

  def apply(puzzle: Puzzle, user: User, result: Result, mobile: Boolean): Fu[(Round, Mode)] = {
    val formerUserRating = user.perfs.puzzle.intRating
    api.head.find(user) flatMap {
      case Some(PuzzleHead(_, Some(c), _)) if c == puzzle.id || mobile =>
        api.head.solved(user, puzzle.id) >> {
          val userRating = user.perfs.puzzle.toRating
          val puzzleRating = puzzle.perf.toRating
          updateRatings(userRating, puzzleRating, result.glicko)
          val date = DateTime.now
          val puzzlePerf = puzzle.perf.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id} user")(puzzleRating)
          val userPerf = user.perfs.puzzle.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id}")(userRating, date)
          val round = new Round(
            id = Round.Id(user.id, puzzle.id),
            date = date,
            result = result,
            rating = formerUserRating,
            ratingDiff = userPerf.intRating - formerUserRating
          )
          (api.round upsert round) >> {
            puzzleColl.update.one(
              $id(puzzle.id),
              $inc(Puzzle.BSONFields.attempts -> $int(1)) ++
                $set(Puzzle.BSONFields.perf -> PuzzlePerf.puzzlePerfBSONHandler.write(puzzlePerf))
            ) zip UserRepo.setPerf(user.id, PerfType.Puzzle, userPerf)
          } inject {
            bus.publish(Puzzle.UserResult(puzzle.id, user.id, result, formerUserRating -> userPerf.intRating), 'finishPuzzle)
            round -> Mode.Rated
          }
        }
      case _ => fuccess {
        incPuzzleAttempts(puzzle)
        new Round(
          id = Round.Id(user.id, puzzle.id),
          date = DateTime.now,
          result = result,
          rating = formerUserRating,
          ratingDiff = 0
        ) -> Mode.Casual
      }
    }
  }

  /* offline solving from the mobile API
   * avoid exploits by not updating the puzzle rating,
   * only the user rating (we don't care about that one).
   * Returns the user with updated puzzle rating */
  def ratedUntrusted(puzzle: Puzzle, user: User, result: Result): Fu[User] = {
    val formerUserRating = user.perfs.puzzle.intRating
    val userRating = user.perfs.puzzle.toRating
    val puzzleRating = puzzle.perf.toRating
    updateRatings(userRating, puzzleRating, result.glicko)
    val date = DateTime.now
    val userPerf = user.perfs.puzzle.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id}")(userRating, date)
    val a = new Round(
      id = Round.Id(user.id, puzzle.id),
      date = date,
      result = result,
      rating = formerUserRating,
      ratingDiff = userPerf.intRating - formerUserRating
    )
    (api.round add a) >>
      UserRepo.setPerf(user.id, PerfType.Puzzle, userPerf) >>-
      bus.publish(
        Puzzle.UserResult(puzzle.id, user.id, result, formerUserRating -> userPerf.intRating),
        'finishPuzzle
      ) inject
        user.copy(perfs = user.perfs.copy(puzzle = userPerf))
  } recover lila.db.recoverDuplicateKey { _ =>
    // logger.info(s"ratedUntrusted ${user.id} ${puzzle.id} duplicate round")
    user // has already been solved!
  }

  private val VOLATILITY = Glicko.default.volatility
  private val TAU = 0.75d
  private val system = new RatingCalculator(VOLATILITY, TAU)

  def incPuzzleAttempts(puzzle: Puzzle): Unit = {
    puzzleColl.update(false, WriteConcern.Unacknowledged).one(
      q = $id(puzzle.id), u = $inc(Puzzle.BSONFields.attempts -> 1)
    )

    ()
  }

  private def updateRatings(u1: Rating, u2: Rating, result: Glicko.Result): Unit = {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(u1, u2)
      case Glicko.Result.Win => results.addResult(u1, u2)
      case Glicko.Result.Loss => results.addResult(u2, u1)
    }
    try {
      val (r1, r2) = (u1.getRating, u2.getRating)
      system.updateRatings(results)
      // never take away more than 30 rating points - it just causes upsets
      List(r1 -> u1, r2 -> u2).foreach {
        case (prev, next) if next.getRating - prev < -30 => next.setRating(prev - 30)
        case _ =>
      }
    } catch {
      case e: Exception => logger.error("finisher", e)
    }
  }
}
