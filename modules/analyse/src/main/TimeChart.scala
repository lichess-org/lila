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
      zipWithZeros(
        game.player(Color.White).moveTimeList,
        game.player(Color.Black).moveTimeList
      ).zipWithIndex map {
        case ((white, black), move) ⇒ Json.arr((move + 1).toString, white, black)
      }
    }
  }

  private def zipWithZeros(a: List[Int], b: List[Int]): List[(Int, Int)] = 
    (a ::: List.fill(b.size - a.size)(0)) zip (b ::: List.fill(a.size - b.size)(0)) 
}

object TimeChart {

  def apply(nameUser: String ⇒ Fu[String])(game: Game): Fu[TimeChart] =
    Future.traverse(game.players) { p ⇒
      Namer.player(p)(nameUser) map (p.color -> _)
    } map { named ⇒ new TimeChart(game, named.toMap) }
}
