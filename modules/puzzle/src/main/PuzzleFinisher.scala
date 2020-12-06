package lila.puzzle

import cats.implicits._
import org.goochjs.glicko2.{ Rating, RatingCalculator, RatingPeriodResults }
import org.joda.time.DateTime
import scala.util.chaining._

import lila.common.Bus
import lila.db.AsyncColl
import lila.db.dsl._
import lila.rating.Perf
import lila.rating.{ Glicko, PerfType }
import lila.user.{ User, UserRepo }

final private[puzzle] class PuzzleFinisher(
    api: PuzzleApi,
    userRepo: UserRepo,
    historyApi: lila.history.HistoryApi,
    colls: PuzzleColls
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def apply(
      puzzle: Puzzle,
      theme: PuzzleTheme.Key,
      user: User,
      result: Result,
      isStudent: Boolean
  ): Fu[(PuzzleRound, Perf)] =
    api.round.find(user, puzzle.id) flatMap { prevRound =>
      val now              = DateTime.now
      val formerUserRating = user.perfs.puzzle.intRating

      val (round, newPuzzleGlicko, userPerf) = prevRound match {
        case Some(prev) =>
          (
            prev.copy(win = result.win),
            none,
            user.perfs.puzzle
          )
        case None =>
          val userRating = user.perfs.puzzle.toRating
          val puzzleRating = new Rating(
            puzzle.glicko.rating atLeast Glicko.minRating,
            puzzle.glicko.deviation,
            puzzle.glicko.volatility,
            puzzle.plays,
            null
          )
          updateRatings(userRating, puzzleRating, result.glicko)
          val newPuzzleGlicko = user.perfs.puzzle.established
            .option {
              val after = Glicko(
                rating = puzzleRating.getRating
                  .atMost(puzzle.glicko.rating + Glicko.maxRatingDelta)
                  .atLeast(puzzle.glicko.rating - Glicko.maxRatingDelta),
                deviation = puzzleRating.getRatingDeviation,
                volatility = puzzleRating.getVolatility
              )
              ponder(theme, result, puzzle.glicko, after)
            }
            .filter(_.sanityCheck)
          val round = PuzzleRound(id = PuzzleRound.Id(user.id, puzzle.id), date = now, win = result.win)
          val userPerf =
            user.perfs.puzzle.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id}")(userRating, now) pipe {
              p =>
                p.copy(glicko = ponder(theme, result, user.perfs.puzzle.glicko, p.glicko))
            }
          (round, newPuzzleGlicko, userPerf)
      }
      api.round.upsert(round) zip
        isStudent.??(api.round.addDenormalizedUser(round, user)) zip
        colls.puzzle {
          _.update
            .one(
              $id(puzzle.id),
              $inc(Puzzle.BSONFields.plays -> $int(1)) ++ newPuzzleGlicko ?? { glicko =>
                $set(Puzzle.BSONFields.glicko -> Glicko.glickoBSONHandler.write(glicko))
              }
            )
            .void
        } zip
        (userPerf != user.perfs.puzzle).?? { userRepo.setPerf(user.id, PerfType.Puzzle, userPerf) } >>-
        Bus.publish(
          Puzzle.UserResult(puzzle.id, user.id, result, formerUserRating -> userPerf.intRating),
          "finishPuzzle"
        ) inject (round -> userPerf)
    }

  private def ponder(theme: PuzzleTheme.Key, result: Result, prev: Glicko, after: Glicko) =
    if (theme == PuzzleTheme.any.key) after
    else if (PuzzleTheme.obviousThemes(theme)) after.average(prev, if (result.win) 0.85f else 0.5f)
    else after.average(prev, if (result.win) 0.6f else 0.4f)

  private val VOLATILITY = Glicko.default.volatility
  private val TAU        = 0.75d
  private val system     = new RatingCalculator(VOLATILITY, TAU)

  def incPuzzlePlays(puzzle: Puzzle): Funit =
    colls.puzzle.map(_.incFieldUnchecked($id(puzzle.id), Puzzle.BSONFields.plays))

  private def updateRatings(u1: Rating, u2: Rating, result: Glicko.Result): Unit = {
    val results = new RatingPeriodResults()
    result match {
      case Glicko.Result.Draw => results.addDraw(u1, u2)
      case Glicko.Result.Win  => results.addResult(u1, u2)
      case Glicko.Result.Loss => results.addResult(u2, u1)
    }
    try {
      val (r1, r2) = (u1.getRating, u2.getRating)
      system.updateRatings(results)
      // never take away more than 30 rating points - it just causes upsets
      List(r1 -> u1, r2 -> u2).foreach {
        case (prev, next) if next.getRating - prev < -30 => next.setRating(prev - 30)
        case _                                           =>
      }
    } catch {
      case e: Exception => logger.error("finisher", e)
    }
  }
}
