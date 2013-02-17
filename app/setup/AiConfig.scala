package lila
package setup

import chess.{ Variant, Mode, Game, Color ⇒ ChessColor }
import game.{ DbGame, DbPlayer }

case class AiConfig(
    variant: Variant,
    clock: Boolean,
    time: Int,
    increment: Int,
    level: Int,
    color: Color,
    fen: Option[String] = None) extends Config with GameGenerator with Positional {

  def >> = (variant.id, clock, time, increment, level, color.name, fen).some

  def game = fenDbGame { chessGame ⇒
    DbGame(
      game = chessGame,
      ai = Some(!creatorColor -> level),
      whitePlayer = DbPlayer(
        color = ChessColor.White,
        aiLevel = creatorColor.black option level),
      blackPlayer = DbPlayer(
        color = ChessColor.Black,
        aiLevel = creatorColor.white option level),
      creatorColor = creatorColor,
      mode = Mode.Casual,
      variant = variant
    )
  } start

  def encode = RawAiConfig(
    v = variant.id,
    k = clock,
    t = time,
    i = increment,
    l = level,
    f = ~fen)
}

object AiConfig extends BaseConfig {

  def <<(v: Int, k: Boolean, t: Int, i: Int, level: Int, c: String, fen: Option[String]) = new AiConfig(
    variant = Variant(v) err "Invalid game variant " + v,
    clock = k,
    time = t,
    increment = i,
    level = level,
    color = Color(c) err "Invalid color " + c,
    fen = fen)

  val default = AiConfig(
    variant = variantDefault,
    clock = false,
    time = 5,
    increment = 8,
    level = 1,
    color = Color.default)

  val levels = (1 to 8).toList

  val levelChoices = levels map { l ⇒ l.toString -> l.toString }
}

private[setup] case class RawAiConfig(
    v: Int,
    k: Boolean,
    t: Int,
    i: Int,
    l: Int,
    f: String = "") {

  def decode = for {
    variant ← Variant(v)
  } yield AiConfig(
    variant = variant,
    clock = k,
    time = t,
    increment = i,
    level = l,
    color = Color.White,
    fen = f.some filter (_.nonEmpty))
}
