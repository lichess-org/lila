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
      result: Result
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
          val newPuzzleGlicko = ponder
            .puzzle(
              theme,
              result,
              puzzle.glicko -> Glicko(
                rating = puzzleRating.getRating
                  .atMost(puzzle.glicko.rating + Glicko.maxRatingDelta)
                  .atLeast(puzzle.glicko.rating - Glicko.maxRatingDelta),
                deviation = puzzleRating.getRatingDeviation,
                volatility = puzzleRating.getVolatility
              ),
              player = user.perfs.puzzle.glicko
            )
            .some
            .filter(puzzle.glicko !=)
            .filter(_.sanityCheck)
          val round =
            PuzzleRound(id = PuzzleRound.Id(user.id, puzzle.id), win = result.win, date = DateTime.now)
          val userPerf =
            user.perfs.puzzle.addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id}")(userRating, now) pipe {
              p =>
                p.copy(glicko =
                  ponder.player(theme, result, user.perfs.puzzle.glicko -> p.glicko, puzzle.glicko)
                )
            }
          (round, newPuzzleGlicko, userPerf)
      }
      api.round.upsert(round, theme) zip
        colls.puzzle {
          _.update
            .one(
              $id(puzzle.id),
              $inc(Puzzle.BSONFields.plays -> $int(1)) ++ newPuzzleGlicko.?? { glicko =>
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

  private object ponder {

    // themes that don't hint at the solution
    private val nonHintingThemes: Set[PuzzleTheme.Key] = Set(
      PuzzleTheme.opening,
      PuzzleTheme.middlegame,
      PuzzleTheme.endgame,
      PuzzleTheme.rookEndgame,
      PuzzleTheme.bishopEndgame,
      PuzzleTheme.pawnEndgame,
      PuzzleTheme.knightEndgame,
      PuzzleTheme.queenEndgame,
      PuzzleTheme.rookEndgame
    ).map(_.key)

    private def isHinting(theme: PuzzleTheme.Key) = !nonHintingThemes(theme)

    // themes that make the solution very obvious
    private val isObvious: Set[PuzzleTheme.Key] = Set(
      PuzzleTheme.enPassant,
      PuzzleTheme.attackingF2F7,
      PuzzleTheme.doubleCheck,
      PuzzleTheme.mateIn1,
      PuzzleTheme.castling
    ).map(_.key)

    private def weightOf(theme: PuzzleTheme.Key, result: Result) =
      if (theme == PuzzleTheme.mix.key) 1
      else if (isObvious(theme)) {
        if (result.win) 0.2f else 0.6f
      } else if (isHinting(theme)) {
        if (result.win) 0.3f else 0.7f
      } else {
        if (result.win) 0.7f else 0.8f
      }

    def player(theme: PuzzleTheme.Key, result: Result, glicko: (Glicko, Glicko), puzzle: Glicko) = {
      val provisionalPuzzle = puzzle.provisional ?? {
        if (result.win) -0.2f else -0.7f
      }
      glicko._1.average(glicko._2, (weightOf(theme, result) + provisionalPuzzle) atLeast 0.1f)
    }

    def puzzle(theme: PuzzleTheme.Key, result: Result, glicko: (Glicko, Glicko), player: Glicko) =
      if (player.clueless) glicko._1
      else glicko._1.average(glicko._2, weightOf(theme, result))
  }

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
