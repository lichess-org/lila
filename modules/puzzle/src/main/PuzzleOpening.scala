package lila.puzzle

import akka.stream.scaladsl._
import chess.opening.FullOpeningDB
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference

import lila.common.LilaStream
import lila.db.dsl._
import lila.game.GameRepo

final class PuzzleOpening(colls: PuzzleColls, gameRepo: GameRepo)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mat: akka.stream.Materializer
) {
  import BsonHandlers._

  def addAllMissing: Funit =
    colls.puzzle {
      _.find($doc(Puzzle.BSONFields.opening $exists false))
        .cursor[Puzzle]()
        .documentSource()
        .mapAsyncUnordered(12)(addMissing)
        .runWith(LilaStream.sinkCount)
        .chronometer
        .log(logger)(count => s"Done adding $count puzzle openings")
        .result
        .void
    }

  private def addMissing(puzzle: Puzzle): Funit = gameRepo game puzzle.gameId flatMap {
    _ ?? { game =>
      FullOpeningDB.search(game.pgnMoves) match {
        case None =>
          fuccess {
            logger warn s"No opening for https://lichess.org/training/${puzzle.id}"
          }
        case Some(o) =>
          val (name, variation) = o.opening.name.split(",") match {
            case Array(name) => (name, "")
            case Array(name) => (name, "")
          colls.puzzle {
            _.updateField($id(puzzle.id), Puzzle.BSONFields.opening, opening).void
          }
      }
    }
  }
}
