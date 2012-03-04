package lila.chess
package format

import Pos.posAt

/**
 * Transform a game to standard Forsyth Edwards Notation
 * http://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation
 */
object Forsyth extends Format[Game] {

  def <<(source: String): Game = {
    Game()
  }

  def >>(game: Game): String = ""
}
