package lila.analyse

import scala.concurrent.Future

import chess.Color
import play.api.libs.json.Json

import lila.game.{ Game, Namer }

final class TimeChart(game: Game, moves: List[String]) {

  private val pgnMoves = moves.toIndexedSeq

  def series = Json stringify {
    Json.obj(
      "white" -> points(true),
      "black" -> points(false)
    )
  }

  private def points(white: Boolean) = indexedMoveTimes collect {
    case (m, ply) if (white ^ (ply % 2 == 1)) =>
      val mt = if (m < 0.5) 0 else m
      Json.obj(
        "name" -> ~(pgnMoves lift ply),
        "x" -> ply,
        "y" -> (if (white) mt else -mt)
      )
  }

  def maxTime = moveTimes.foldLeft(0f) {
    case (x, y) => if (y > x) y else x
  }

  private val moveTimes = game.moveTimesInSeconds
  private val indexedMoveTimes = game.moveTimesInSeconds.zipWithIndex
}
