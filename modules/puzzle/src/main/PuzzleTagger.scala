package lila.puzzle

import chess.format.{ Forsyth, Uci }
import chess.{ Divider, Division }
import reactivemongo.akkastream.cursorProducer
import scala.concurrent.duration._

import lila.common.LilaStream
import lila.db.dsl._

final private class PuzzleTagger(colls: PuzzleColls, openingApi: PuzzleOpeningApi)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {
  import BsonHandlers._

  private[puzzle] def addAllMissing: Funit =
    colls.puzzle {
      _.find($doc(Puzzle.BSONFields.tagMe -> true))
        .cursor[Puzzle]()
        .documentSource()
        .throttle(500, 1 second)
        .mapAsyncUnordered(2)(p => openingApi.updateOpening(p) inject p)
        .mapAsyncUnordered(2)(addPhase)
        .runWith(LilaStream.sinkCount)
        .chronometer
        .log(logger)(count => s"Done tagging $count puzzles")
        .result
        .void
    }

  private def addPhase(puzzle: Puzzle): Funit =
    puzzle.situationAfterInitialMove match {
      case Some(sit) =>
        val theme = Divider(List(sit.board)) match {
          case Division(None, Some(_), _) => PuzzleTheme.endgame
          case Division(Some(_), None, _) => PuzzleTheme.middlegame
          case _                          => PuzzleTheme.opening
        }
        colls.puzzle {
          _.update
            .one(
              $id(puzzle.id),
              $addToSet(Puzzle.BSONFields.themes -> theme.key) ++ $unset(Puzzle.BSONFields.tagMe)
            )
            .void
        }
      case None =>
        logger.error(s"Can't compute phase of puzzle $puzzle")
        funit
    }
}
