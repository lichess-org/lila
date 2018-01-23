package lila.setup

import chess.Mode
import lila.game.{ Game, Player, Source }
import lila.lobby.Color

private[setup] case object ApiConfig extends Config {

  val color = Color.White
  val variant = chess.variant.Standard
  val mode = Mode.Casual
  val timeMode = TimeMode.Unlimited
  val time = 5d
  val increment = 8
  val days = 2

  def game = Game.make(
    chess = makeGame,
    whitePlayer = Player.white,
    blackPlayer = Player.black,
    mode = mode,
    source = Source.Api,
    pgnImport = None
  )
}
