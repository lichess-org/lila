package lila
package analyse

import game.{ DbPlayer, DbGame }

import scala.math.round
import com.codahale.jerkson.Json

final class TimeChart(game: DbGame) {

  def columns = Json generate {
    List("string", "Move") :: game.players.map(p ⇒
      List("number", "%s - %s".format(p.color, p.userId | "anonymous")))
  }

  def rows = Json generate {
    (game.creator.moveTimeList zip game.invited.moveTimeList).zipWithIndex map {
      case ((black, white), move) ⇒ List(move.toString, white, black)
    }
  }
}
