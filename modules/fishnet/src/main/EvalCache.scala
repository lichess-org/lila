package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._

import chess.format.FEN
import chess.variant.Standard
import JsonApi.Request.Evaluation
import lila.db.dsl._

private final class EvalCache(coll: Coll) {

  val MAX_PLIES = 6

  import BSONHandlers._
  import EvalCache._
  import Evaluation.Score
  private implicit val EvaluationScoreBSONHandler = Macros.handler[Score]
  private implicit val EvaluationBSONHandler = Macros.handler[Evaluation]
  private implicit val EntryBSONHandler = Macros.handler[Entry]

  def get(fen: FEN): Fu[Option[Evaluation]] =
    coll.primitiveOne[Evaluation]($id(fen), "eval")

  def exists(fen: FEN): Fu[Boolean] = coll.exists($id(fen))

  def save(work: Work.Analysis, evals: List[Evaluation], game: lila.game.Game): Funit =
    cacheable(work, game) ?? {
      evals.take(MAX_PLIES - work.startPly).takeWhile(_.isRoughlyEqual) match {
        case Nil => funit
        case firstEvals => firstFens(game, work.startPly, firstEvals.size).flatMap {
          _.zip(firstEvals).map((set _).tupled).sequenceFu.void
        }
      }
    }

  def reduce(game: lila.game.Game)(work: Work.Analysis): Fu[Work.Analysis] =
    if (!cacheable(work, game)) fuccess(work)
    else firstFens(game, 0, MAX_PLIES + 1).flatMap { gameFens =>
      gameFens.foldLeft(fuccess(Vector.empty[FEN] -> true)) {
        case (res, fen) => res.flatMap {
          case (fens, true) => exists(fen).map {
            case true  => (fens :+ fen, true)
            case false => (fens, false)
          }
          case res => fuccess(res)
        }
      }.map(_._1) map {
        _.foldLeft(work) {
          case (w, cachedFen) => gameFens.lift(w.startPly + 1).fold(w) { nextFen =>
            w.copy(
              startPly = w.startPly + 1,
              game = w.game.dropFirstMove.copy(initialFen = nextFen))
          }
        }
      }
    }

  private def firstFens(game: lila.game.Game, from: Int, nb: Int): Fu[List[FEN]] =
    chess.Replay.games(
      game.pgnMoves.take(from + nb),
      Standard.initialFen.some,
      Standard).fold(
        fufail(_),
        games => fuccess {
          games.drop(from).map(chess.format.Forsyth.>>).map(FEN.apply)
        })

  private def set(fen: FEN, eval: Evaluation): Funit =
    coll.update($id(fen), Entry(fen, eval, DateTime.now), upsert = true).void

  private def cacheable(work: Work.Analysis, game: lila.game.Game) =
    game.variant.standard && work.startPly < MAX_PLIES
}

private object EvalCache {

  case class Entry(
    _id: FEN,
    eval: Evaluation,
    date: DateTime)
}
