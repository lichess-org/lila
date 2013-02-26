package lila
package setup

import chess.{ Variant, Mode, Clock, Color ⇒ ChessColor }
import elo.EloRange
import game.{ DbGame, DbPlayer, Source }

case class FriendConfig(
    variant: Variant,
    clock: Boolean,
    time: Int,
    increment: Int,
    mode: Mode,
    color: Color,
    fen: Option[String] = None) extends HumanConfig with GameGenerator with Positional {

  def >> = (variant.id, clock, time, increment, mode.id.some, color.name, fen).some

  def game = fenDbGame { chessGame ⇒
    DbGame(
      game = chessGame,
      ai = None,
      whitePlayer = DbPlayer.white,
      blackPlayer = DbPlayer.black,
      creatorColor = creatorColor,
      mode = mode,
      variant = variant,
      source = Source.Friend,
      pgnImport = None)
  }

  def encode = RawFriendConfig(
    v = variant.id,
    k = clock,
    t = time,
    i = increment,
    m = mode.id,
    f = ~fen)
}

object FriendConfig extends BaseHumanConfig {

  def <<(v: Int, k: Boolean, t: Int, i: Int, m: Option[Int], c: String, fen: Option[String]) =
    new FriendConfig(
      variant = Variant(v) err "Invalid game variant " + v,
      clock = k,
      time = t,
      increment = i,
      mode = m.fold(Mode.orDefault, Mode.default),
      color = Color(c) err "Invalid color " + c,
      fen = fen)

  val default = FriendConfig(
    variant = variantDefault,
    clock = true,
    time = 5,
    increment = 8,
    mode = Mode.default,
    color = Color.default)
}

private[setup] case class RawFriendConfig(
    v: Int,
    k: Boolean,
    t: Int,
    i: Int,
    m: Int,
    f: String = "") {

  def decode = for {
    variant ← Variant(v)
    mode ← Mode(m)
  } yield FriendConfig(
    variant = variant,
    clock = k,
    time = t,
    increment = i,
    mode = mode,
    color = Color.White,
    fen = f.some filter (_.nonEmpty))
}
