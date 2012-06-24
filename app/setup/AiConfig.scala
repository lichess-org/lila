package lila
package setup

import chess.{ Game, Board, Variant, Mode, Color ⇒ ChessColor }
import game.{ DbGame, DbPlayer }

case class AiConfig(
    variant: Variant,
    level: Int,
    color: Color) extends Config with GameGenerator {

  def >> = (variant.id, level, color.name).some

  def game = DbGame(
    game = Game(board = Board init variant),
    ai = Some(!creatorColor -> level),
    whitePlayer = DbPlayer(
      color = ChessColor.White,
      aiLevel = creatorColor.black option level),
    blackPlayer = DbPlayer(
      color = ChessColor.Black,
      aiLevel = creatorColor.white option level),
    creatorColor = creatorColor,
    mode = Mode.Casual,
    variant = variant).start

  def encode = RawAiConfig(
    v = variant.id,
    l = level)
}

object AiConfig extends BaseConfig {

  def <<(v: Int, level: Int, c: String) = new AiConfig(
    variant = Variant(v) err "Invalid game variant " + v,
    level = level,
    color = Color(c) err "Invalid color " + c)

  val default = AiConfig(
    variant = variantDefault,
    level = 1,
    color = Color.default)

  val levels = (1 to 8).toList

  val levelChoices = levels map { l ⇒ l.toString -> l.toString }
}

case class RawAiConfig(
    v: Int,
    l: Int) {

  def decode = for {
    variant ← Variant(v)
  } yield AiConfig(
    variant = variant,
    level = l,
    color = Color.White)
}
