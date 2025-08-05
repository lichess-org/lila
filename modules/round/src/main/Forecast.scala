package lila.round

import chess.format.pgn.SanStr
import chess.format.{ Fen, Uci }
import chess.{ Move, Ply }
import play.api.libs.json.*

import lila.common.Json.given

case class Forecast(_id: GameFullId, steps: Forecast.Steps, date: Instant):

  def apply(g: Game, lastMove: Move): Option[(Forecast, Uci.Move)] =
    nextMove(g, lastMove).map { move =>
      copy(
        steps = steps.collect {
          case fst :: snd :: rest if rest.nonEmpty && g.ply == fst.ply && fst.is(lastMove) && snd.is(move) =>
            rest
        },
        date = nowInstant
      ) -> move
    }

  // accept up to 30 lines of 30 moves each
  def truncate = copy(steps = steps.take(30).map(_.take(30)))

  private def nextMove(g: Game, last: Move) =
    steps.collectFirstSome:
      case fst :: snd :: _ if g.ply == fst.ply && fst.is(last) => snd.uciMove
      case _ => none

object Forecast:

  type Steps = List[List[Step]]

  def maxPlies(steps: Steps): Int = steps.foldLeft(0)(_ max _.size)

  case class Step(
      ply: Ply,
      uci: String,
      san: SanStr,
      fen: Fen.Full,
      check: Option[Boolean]
  ):

    def is(move: Move) = move.toUci.uci == uci
    def is(move: Uci.Move) = move.uci == uci

    def uciMove = Uci.Move(uci)

  given Format[Step] = Json.format
  given Writes[Forecast] = Json.writes

  case object OutOfSync extends lila.core.lilaism.LilaException:
    val message = "Forecast out of sync"
