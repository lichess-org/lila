package lila.puzzle

import org.goochjs.glicko2.{ Rating, RatingCalculator, RatingPeriodResults }
import org.joda.time.DateTime

import chess.Mode
import lila.common.Bus
import lila.db.AsyncColl
import lila.db.dsl._
import lila.rating.{ Glicko, PerfType }
import lila.user.{ User, UserRepo }

final private[puzzle] class Finisher(
    api: PuzzleApi,
    userRepo: UserRepo,
    historyApi: lila.history.HistoryApi,
    puzzleColl: AsyncColl
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(
      puzzle: Puzzle,
      user: User,
      result: Result,
      mobile: Boolean,
      isStudent: Boolean
  ): Fu[(Round, Mode)] = ???
  // val formerUserRating = user.perfs.puzzle.intRating
  // api.head.find(user) flatMap {
  //   case Some(PuzzleHead(_, Some(c), _)) if c == puzzle.id || mobile =>
  //     api.head.solved(user, puzzle.id) >> {
  //       val userRating   = user.perfs.puzzle.toRating
  //       val puzzleRating = puzzle.perf.toRating
  //       updateRatings(userRating, puzzleRating, result.glicko)
  //       val date = DateTime.now
  //       val puzzlePerf =
  //         puzzle.perf.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id} user")(puzzleRating)
  //       val userPerf =
  //         user.perfs.puzzle.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id}")(userRating, date)
  //       val round = new Round(
  //         id = Round.Id(user.id, puzzle.id),
  //         date = date,
  //         result = result,
  //         rating = formerUserRating,
  //         ratingDiff = userPerf.intRating - formerUserRating
  //       )
  //       historyApi.addPuzzle(user = user, completedAt = date, perf = userPerf)
  //       (api.round upsert round) >> {
  //         isStudent ?? api.round.addDenormalizedUser(round, user)
  //       } >> {
  //         puzzleColl {
  //           _.update.one(
  //             $id(puzzle.id),
  //             $inc(Puzzle.BSONFields.attempts -> $int(1)) ++
  //               $set(Puzzle.BSONFields.perf   -> PuzzlePerf.puzzlePerfBSONHandler.write(puzzlePerf))
  //           )
  //         } zip userRepo.setPerf(user.id, PerfType.Puzzle, userPerf)
  //       } inject {
  //         Bus.publish(
  //           Puzzle.UserResult(puzzle.id, user.id, result, formerUserRating -> userPerf.intRating),
  //           "finishPuzzle"
  //         )
  //         round -> Mode.Rated
  //       }
  //     }
  //   case _ =>
  //     incPuzzleAttempts(puzzle) inject new Round(
  //       id = Round.Id(user.id, puzzle.id),
  //       date = DateTime.now,
  //       result = result,
  //       rating = formerUserRating,
  //       ratingDiff = 0
  //     ) -> Mode.Casual
  // }
  // }

  private val VOLATILITY = Glicko.default.volatility
  private val TAU        = 0.75d
  private val system     = new RatingCalculator(VOLATILITY, TAU)

  def incPuzzlePlays(puzzle: Puzzle): Funit =
    puzzleColl.map(_.incFieldUnchecked($id(puzzle.id.value), Puzzle.BSONFields.plays))

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
