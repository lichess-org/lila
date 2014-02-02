package lila.analyse

import scala.concurrent.Future

import chess.Color
import play.api.libs.json.Json

import lila.game.{ Game, Namer }

final class TimeChart(game: Game, usernames: Map[Color, String]) {

  def series = Json stringify {
    Json.obj(
      "white" -> moveTimes(0),
      "black" -> moveTimes(1).map(-_)
    )
  }

  private val indexedMoveTimes = game.moveTimesInSeconds.zipWithIndex
  private def moveTimes(i: Int) =
    indexedMoveTimes.view.filter(_._2 % 2 == i).map(_._1).toList map { x ⇒
      if (x < 0.5) 0 else x
    }
}

object TimeChart {

  def apply(nameUser: String ⇒ Fu[String])(game: Game): Fu[TimeChart] =
    Future.traverse(game.players) { p ⇒
      Namer.player(p)(nameUser) map (p.color -> _)
    } map { named ⇒ new TimeChart(game, named.toMap) }
}
