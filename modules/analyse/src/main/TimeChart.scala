package lila.analyse

import scala.concurrent.Future

import chess.Color
import play.api.libs.json.Json

import lila.game.{ Game, Namer }

final class TimeChart(game: Game) {

  def series = Json stringify {
    Json.obj(
      "white" -> moveTimesOf(0),
      "black" -> moveTimesOf(1).map(-_)
    )
  }

  private val moveTimes = game.moveTimesInSeconds
  private val indexedMoveTimes = moveTimes.zipWithIndex
  private def moveTimesOf(i: Int) =
    indexedMoveTimes.view.filter(_._2 % 2 == i).map(_._1).toList map { x =>
      if (x < 0.5) 0 else x
    }
  def maxTime = moveTimes.map(math.abs).foldLeft(0f) {
    case (x, y) => if (y > x) y else x
  }
}
