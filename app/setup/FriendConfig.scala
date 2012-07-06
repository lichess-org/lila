package lila
package setup

import chess.{ Game, Board, Variant, Mode, Clock, Color ⇒ ChessColor }
import elo.EloRange
import game.{ DbGame, DbPlayer }

case class FriendConfig(
    variant: Variant,
    clock: Boolean,
    time: Int,
    increment: Int,
    mode: Mode,
    color: Color) extends HumanConfig with GameGenerator {

  def >> = (variant.id, clock, time, increment, mode.id.some, color.name).some

  def game = DbGame(
    game = Game(
      board = Board init variant,
      clock = makeClock),
    ai = None,
    whitePlayer = DbPlayer.white,
    blackPlayer = DbPlayer.black,
    creatorColor = creatorColor,
    mode = mode,
    variant = variant)

  def encode = RawFriendConfig(
    v = variant.id,
    k = clock,
    t = time,
    i = increment,
    m = mode.id)
}

object FriendConfig extends BaseHumanConfig {

  def <<(v: Int, k: Boolean, t: Int, i: Int, m: Option[Int], c: String) =
    new FriendConfig(
      variant = Variant(v) err "Invalid game variant " + v,
      clock = k,
      time = t,
      increment = i,
      mode = m.fold(Mode.orDefault, Mode.default),
      color = Color(c) err "Invalid color " + c)

  val default = FriendConfig(
    variant = variantDefault,
    clock = true,
    time = 5,
    increment = 8,
    mode = Mode.default,
    color = Color.default)
}

case class RawFriendConfig(
    v: Int,
    k: Boolean,
    t: Int,
    i: Int,
    m: Int) {

  def decode = for {
    variant ← Variant(v)
    mode ← Mode(m)
  } yield FriendConfig(
    variant = variant,
    clock = k,
    time = t,
    increment = i,
    mode = mode,
    color = Color.White)
}
