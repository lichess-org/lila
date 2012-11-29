package lila
package analyse

import game.{ DbPlayer, DbGame, Namer }
import chess.Color.{ White, Black }

import scala.math.round
import play.api.libs.json.Json

final class TimeChart(game: DbGame) {

  def columns = Json stringify {
    Json toJson {
      List("string", "Move") :: game.players.map(p ⇒
        List("number", "%s - %s".format(p.color, Namer.player(p)(identity))))
    }
  }

  def rows = Json stringify {
    Json toJson {
      (game.player(White).moveTimeList zip game.player(Black).moveTimeList).zipWithIndex map {
        case ((white, black), move) ⇒ List(move.toString, white, black)
      }
    }
  }
}
