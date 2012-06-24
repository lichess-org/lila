package lila
package setup

import chess.{ Game, Board, Variant, Mode, Clock, Color ⇒ ChessColor }
import elo.EloRange
import game.{ DbGame, DbPlayer }

case object ApiConfig extends Config with GameGenerator {

  val color = Color.White
  val variant = Variant.Standard
  val mode = Mode.Casual

  def game = DbGame(
    game = Game(
      board = Board init variant,
      clock = none),
    ai = None,
    whitePlayer = DbPlayer.white,
    blackPlayer = DbPlayer.black,
    creatorColor = creatorColor,
    mode = mode,
    variant = variant)
}
