package lila
package setup

import chess.{ Variant, Mode, Clock, Color ⇒ ChessColor }
import elo.EloRange
import game.{ DbGame, DbPlayer, Source }

case object ApiConfig extends Config with GameGenerator {

  val color = Color.White
  val variant = Variant.Standard
  val mode = Mode.Casual
  val clock = false
  val time = 5
  val increment = 8

  def game = DbGame(
    game = makeGame,
    ai = None,
    whitePlayer = DbPlayer.white,
    blackPlayer = DbPlayer.black,
    creatorColor = creatorColor,
    mode = mode,
    variant = variant,
    source = Source.Api)
}
