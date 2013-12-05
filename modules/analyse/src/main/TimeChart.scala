package lila.analyse

import scala.concurrent.Future

import play.api.libs.json.Json

import chess.Color
import lila.game.{ Game, Namer }

final class TimeChart(game: Game, usernames: Map[Color, String]) {

  def columns = Json stringify {
    Json toJson {
      List("string", "Move") :: (usernames.toList map {
        case (color, name) ⇒ List("number", "%s - %s".format(color, name))
      })
    }
  }

  def rows = Json stringify {
    Json toJson {
      (moveTimes(0) zip moveTimes(1)).zipWithIndex map {
        case ((white, black), move) ⇒ Json.arr((move + 1).toString, white, black)
      }
    }
  }

  private val indexedMoveTimes = game.moveTimesInSeconds.zipWithIndex
  private def moveTimes(i: Int) = 
    indexedMoveTimes.view.filter(_._2 % 2 == i).map(_._1).toList map { x =>
      if (x < 0.5) 0 else x
    }
}

object TimeChart {

  def apply(nameUser: String ⇒ Fu[String])(game: Game): Fu[TimeChart] =
    Future.traverse(game.players) { p ⇒
      Namer.player(p)(nameUser) map (p.color -> _)
    } map { named ⇒ new TimeChart(game, named.toMap) }
}
