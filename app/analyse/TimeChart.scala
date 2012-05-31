package lila
package analyse

import game.{ DbPlayer, DbGame, Namer }
import chess.Color.{ White, Black }

import scala.math.round
import com.codahale.jerkson.Json

final class TimeChart(game: DbGame) {

  def columns = Json generate {
    List("string", "Move") :: game.players.map(p ⇒
      List("number", "%s - %s".format(p.color, Namer.player(p)(identity))))
  }

  def rows = Json generate {
    (game.player(White).moveTimeList zip game.player(Black).moveTimeList).zipWithIndex map {
      case ((white, black), move) ⇒ List(move.toString, white, black)
    }
  }
}
