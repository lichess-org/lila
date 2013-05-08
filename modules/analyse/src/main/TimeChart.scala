package lila.analyse

import lila.game.{ Game, Namer }
import chess.Color

import scala.concurrent.Future
import play.api.libs.json.Json

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
      (game.player(Color.White).moveTimeList zip game.player(Color.Black).moveTimeList).zipWithIndex map {
        case ((white, black), move) ⇒ Json.arr(move.toString, white, black)
      }
    }
  }
}

private[analyse] object TimeChart {

  def apply(nameUser: String ⇒ Fu[String])(game: Game): Fu[TimeChart] =
    Future.traverse(game.players) { p ⇒
      Namer.player(p)(nameUser) map (p.color -> _)
    } map { named ⇒ new TimeChart(game, named.toMap) }
}
