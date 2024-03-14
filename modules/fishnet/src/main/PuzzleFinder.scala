package lila.fishnet

import org.joda.time.DateTime

import shogi.Replay

import lila.analyse.Analysis
import lila.tree.Eval
import lila.tree.Eval.{ Cp, Mate }

// finds candidates for puzzle from analysis
private object PuzzleFinder {

  def apply(work: Work.Analysis, analysis: Analysis): List[Work.Puzzle] =
    if (
      work.puzzleWorthy &&
      work.isStandard &&
      work.game.initialSfen.fold(true)(_.initialOf(work.game.variant)) &&
      work.game.studyId.isEmpty
    ) {
      lazy val games = Replay
        .gamesWhileValid(
          work.game.usiList,
          work.game.initialSfen,
          work.game.variant
        )
        ._1
        .toList
      val investigatable = analysis.infos
        .map(_.eval)
        .sliding(2)
        .zipWithIndex
        .drop(10) // no need to look at the first few positions
        .collect {
          case (List(prev, next), index) if (shouldInvestigate(prev, next)) =>
            index + 1
        }
        .toList
        .takeRight(5)

      investigatable flatMap { index =>
        games.lift(index).map { game =>
          Work.Puzzle(
            _id = Work.makeId,
            game = Work.Game(
              id = "synthetic",
              initialSfen = game.toSfen.some,
              studyId = none,
              variant = game.variant,
              moves = ~work.game.usiList.lift(index).map(_.usi)
            ),
            engine = lila.game.EngineConfig.Engine.YaneuraOu.name,
            source = Work.Puzzle.Source(
              game = Work.Puzzle.Source
                .FromGame(
                  id = work.game.id
                )
                .some,
              user = none
            ),
            tries = 0,
            lastTryByKey = none,
            acquired = none,
            createdAt = DateTime.now,
            verifiable = false
          )
        }
      }
    } else Nil

  // requires some tuning
  private def shouldInvestigate(prev: Eval, next: Eval): Boolean =
    ~(for {
      a <- prev.score
      b <- next.score
    } yield {
      (a.value, b.value) match {
        case (Left(Cp(a_cp)), Left(Cp(b_cp))) =>
          // From even to winning
          (Math.abs(a_cp) < 350 && Math.abs(b_cp - a_cp) >= 1250) ||
          // From winning to even
          (Math.abs(a_cp) >= 2000 && Math.abs(b_cp) < 300) ||
          // From winning to losing
          (Math.abs(a_cp) >= 1250 && Math.abs(b_cp) >= 750 && a_cp.sign != b_cp.sign)
        case (Left(Cp(a_cp)), Right(Mate(b_mate))) =>
          // From even to checkmate
          (Math.abs(a_cp) < 500) ||
          // From winning to checkmate
          (a_cp.sign != b_mate.sign)
        case (Right(Mate(a_mate)), Left(Cp(b_cp))) =>
          // Checkmate to losing
          a_mate.sign != b_cp.sign ||
          // Checkmate to even
          Math.abs(b_cp) < 500
        case (Right(Mate(a_mate)), Right(Mate(b_mate))) =>
          // From checkmating to being checkmated
          (b_mate != 0 && a_mate.sign != b_mate.sign)
        case _ => false
      }
    })
}
