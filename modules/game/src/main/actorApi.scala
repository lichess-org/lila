package lila.game
package actorApi
import chess.format.Fen

import lila.core.game.{ Game, Pov }

case class MoveGameEvent(
    game: Game,
    fen: Fen.Full,
    move: String
)
object MoveGameEvent:
  def makeChan(gameId: GameId) = s"moveEvent:$gameId"

case class BoardDrawOffer(game: Game)
object BoardDrawOffer:
  def makeChan(gameId: GameId) = s"boardDrawOffer:$gameId"

case class BoardTakeback(game: Game)
object BoardTakeback:
  def makeChan(gameId: GameId) = s"boardTakeback:$gameId"

case class BoardTakebackOffer(game: Game)
object BoardTakebackOffer:
  def makeChan = BoardTakeback.makeChan

case class BoardGone(pov: Pov, claimInSeconds: Option[Int])
object BoardGone:
  def makeChan(gameId: GameId) = s"boardGone:$gameId"

case class NotifyRematch(newGame: Game)
