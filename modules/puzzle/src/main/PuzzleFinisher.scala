package lila.puzzle

import chess.{ Mode, ByColor }
import chess.rating.IntRatingDiff
import chess.rating.glicko.{ Glicko, GlickoCalculator }
import scalalib.actor.AsyncActorSequencers

import lila.common.Bus
import lila.core.perf.Perf
import lila.db.dsl.{ *, given }
import lila.puzzle.PuzzleForm.batch.Solution
import lila.rating.GlickoExt.{ cap, sanityCheck }
import lila.rating.PerfExt.*
import lila.rating.PerfType

final private[puzzle] class PuzzleFinisher(
    api: PuzzleApi,
    userApi: lila.core.user.UserApi,
    historyApi: lila.core.history.HistoryApi,
    colls: PuzzleColls
)(using Executor)(using scheduler: Scheduler):

  private val sequencer = AsyncActorSequencers[PuzzleId](
    maxSize = Max(64),
    expiration = 5.minutes,
    timeout = 5.seconds,
    name = "puzzle.finish",
    lila.log.asyncActorMonitor.full
  )

  private val calculator = GlickoCalculator()

  def batch(
      angle: PuzzleAngle,
      solutions: List[Solution]
  )(using me: Me, perf: Perf): Fu[List[(PuzzleRound, IntRatingDiff)]] =
    solutions
      .foldM((perf, List.empty[(PuzzleRound, IntRatingDiff)])):
        case ((perf, rounds), sol) =>
          apply(sol.id, angle, sol.win, sol.mode)(using me, perf).map:
            case Some(round, newPerf) =>
              val rDiff = IntRatingDiff(newPerf.intRating.value - perf.intRating.value)
              (newPerf, (round, rDiff) :: rounds)
            case None => (perf, rounds)
      .map: (_, rounds) =>
        rounds.reverse

  def apply(
      id: PuzzleId,
      angle: PuzzleAngle,
      win: PuzzleWin,
      mode: Mode
  )(using me: Me, perf: Perf): Fu[Option[(PuzzleRound, Perf)]] =
    if api.casual(me.value, id) then
      fuccess:
        some:
          PuzzleRound(
            id = PuzzleRound.Id(me.userId, id),
            win = win,
            fixedAt = none,
            date = nowInstant
          ) -> perf
    else
      sequencer(id):
        api.round
          .find(me.value, id)
          .zip(api.puzzle.find(id))
          .flatMap:
            case (_, None) => fuccess(none)
            case (prevRound, Some(puzzle)) =>
              val now = nowInstant
              prevRound
                .match
                  case Some(prev) =>
                    fuccess:
                      (prev.updateWithWin(win), none, perf)
                  case None if mode.casual =>
                    fuccess:
                      val round = PuzzleRound(
                        id = PuzzleRound.Id(me.userId, puzzle.id),
                        win = win,
                        fixedAt = none,
                        date = now
                      )
                      (round, none, perf)
                  case None =>
                    // for rating computation, we treat the solve as a game
                    // where the player is white and the puzzle is black
                    val (userGlicko, puzzleGlicko) =
                      val players = ByColor(
                        perf.toGlickoPlayer,
                        chess.rating.glicko.Player(puzzle.glicko.cap, puzzle.plays, none)
                      )
                      calculator
                        .computeGame:
                          chess.rating.glicko.Game(players, chess.Outcome(Color.fromWhite(win.yes).some))
                        .map(_.map(_.glicko))
                        .fold(
                          err =>
                            logger.error(s"Failed to compute glicko for puzzle ${puzzle.id}", err)
                            players.map(_.glicko).toPair
                          ,
                          _.toPair
                        )
                    userApi
                      .dubiousPuzzle(me.userId, perf)
                      .map: dubiousPuzzleRating =>
                        val newPuzzleGlicko = (!dubiousPuzzleRating).so(
                          ponder
                            .puzzle(
                              angle,
                              win,
                              puzzle.glicko -> puzzleGlicko
                                .copy(
                                  rating = puzzleGlicko.rating
                                    .atMost(puzzle.glicko.rating + lila.rating.Glicko.maxRatingDelta)
                                    .atLeast(puzzle.glicko.rating - lila.rating.Glicko.maxRatingDelta)
                                )
                                .cap,
                              player = perf.glicko
                            )
                            .some
                            .filter(puzzle.glicko !=)
                            .filter(_.sanityCheck)
                        )
                        val round =
                          PuzzleRound(
                            id = PuzzleRound.Id(me.userId, puzzle.id),
                            win = win,
                            fixedAt = none,
                            date = now
                          )
                        val userPerf = perf
                          .addOrReset(_.puzzle.crazyGlicko, s"puzzle ${puzzle.id}")(userGlicko, now)
                          .pipe: p =>
                            p.copy(glicko = ponder.player(angle, win, perf.glicko -> p.glicko, puzzle.glicko))
                        (round, newPuzzleGlicko, userPerf)
                .flatMap: (round, newPuzzleGlicko, userPerf) =>
                  import lila.rating.Glicko.glickoHandler
                  for
                    _ <- api.round
                      .upsert(round, angle)
                      .zip:
                        colls.puzzle:
                          _.update
                            .one(
                              $id(puzzle.id),
                              $inc(Puzzle.BSONFields.plays -> $int(1)) ++ newPuzzleGlicko.so { glicko =>
                                $set(Puzzle.BSONFields.glicko -> glicko)
                              }
                            )
                      .zip:
                        (userPerf != perf).so:
                          userApi
                            .setPerf(me.userId, PerfType.Puzzle, userPerf.clearRecent)
                            .zip(historyApi.addPuzzle(user = me.value, completedAt = now, perf = userPerf))
                            .void
                    _ = if prevRound.isEmpty then
                      Bus.publish(
                        Puzzle
                          .UserResult(
                            puzzle.id,
                            me.userId,
                            win,
                            perf.intRating -> userPerf.intRating
                          ),
                        "finishPuzzle"
                      )
                  yield (round -> userPerf).some

  private object ponder:

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
      PuzzleTheme.queenRookEndgame,
      PuzzleTheme.master,
      PuzzleTheme.masterVsMaster,
      PuzzleTheme.superGM
    ).map(_.key)

    private def isHinting(theme: PuzzleTheme.Key) = !nonHintingThemes(theme)

    // themes that make the solution very obvious
    private val isObvious: Set[PuzzleTheme.Key] = Set(
      PuzzleTheme.enPassant,
      PuzzleTheme.attackingF2F7,
      PuzzleTheme.doubleCheck,
      PuzzleTheme.mateIn1,
      PuzzleTheme.castling
    ).map(_.key) ++ PuzzleTheme.allMates

    private def weightOf(angle: PuzzleAngle, win: PuzzleWin) =
      angle.asTheme.fold(1f): theme =>
        if theme == PuzzleTheme.mix.key then 1
        else if isObvious(theme) then if win.yes then 0.1f else 0.4f
        else if isHinting(theme) then if win.yes then 0.2f else 0.7f
        else if win.yes then 0.7f
        else 0.8f

    def player(angle: PuzzleAngle, win: PuzzleWin, glicko: (Glicko, Glicko), puzzle: Glicko) =
      val provisionalPuzzle = puzzle.provisional.yes.so:
        if win.yes then -0.2f else -0.7f
      glicko._1.average(glicko._2, (weightOf(angle, win) + provisionalPuzzle).atLeast(0.1f))

    def puzzle(angle: PuzzleAngle, win: PuzzleWin, glicko: (Glicko, Glicko), player: Glicko) =
      if player.clueless then glicko._1
      else glicko._1.average(glicko._2, weightOf(angle, win))

  def incPuzzlePlays(puzzleId: PuzzleId): Funit =
    colls.puzzle.map(_.incFieldUnchecked($id(puzzleId), Puzzle.BSONFields.plays))
