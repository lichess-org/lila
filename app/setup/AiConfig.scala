package lila
package setup

import chess.{ Game, Board, Variant, Color ⇒ ChessColor }
import elo.EloRange
import game.{ DbGame, DbPlayer }

import org.joda.time.DateTime

case class AiConfig(variant: Variant, level: Int, color: Color) extends Config {

  def >> = (variant.id, level, color.name).some

  def game = DbGame(
    game = Game(board = Board(pieces = variant.pieces)),
    ai = Some(!creatorColor -> level),
    whitePlayer = DbPlayer(
      color = ChessColor.White,
      aiLevel = creatorColor.black option level),
    blackPlayer = DbPlayer(
      color = ChessColor.Black,
      aiLevel = creatorColor.white option level),
    creatorColor = creatorColor,
    isRated = false,
    variant = variant,
    createdAt = DateTime.now)
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

  val levelChoices = (1 to 8).toList map { l ⇒ l.toString -> l.toString }
}

//case class HookConfig(eloRange: Option[String]) 
//extends HumanConfig with EloRange 
