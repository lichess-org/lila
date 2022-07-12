package lila.puzzle

import chess.Divider
import chess.format.{ Forsyth, Uci }
import reactivemongo.akkastream.cursorProducer

import lila.common.LilaStream
import lila.db.dsl._
import chess.Division

final private class PuzzlePhaser(colls: PuzzleColls)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mat: akka.stream.Materializer
) {
  import BsonHandlers._

  private[puzzle] def addAllMissing: Funit =
    colls.puzzle {
      _.find(
        $doc(
          Puzzle.BSONFields.themes $nin List(
            PuzzleTheme.opening.key,
            PuzzleTheme.middlegame.key,
            PuzzleTheme.endgame.key
          )
        )
      )
        .cursor[Puzzle]()
        .documentSource()
        .mapAsyncUnordered(2)(addMissing)
        .runWith(LilaStream.sinkCount)
        .chronometer
        .log(logger)(count => s"Done adding $count puzzle phases")
        .result
        .void
    }

  private def addMissing(puzzle: Puzzle): Funit =
    puzzle.situationAfterInitialMove match {
      case Some(sit) =>
        val theme = Divider(List(sit.board)) match {
          case Division(None, Some(_), _) => PuzzleTheme.endgame
          case Division(Some(_), None, _) => PuzzleTheme.middlegame
          case _                          => PuzzleTheme.opening
        }
        colls.puzzle {
          _.update.one($id(puzzle.id), $push(Puzzle.BSONFields.themes -> theme.key)).void
        }
      case None =>
        logger.error(s"Can't compute phase of puzzle $puzzle")
        funit
    }
}
