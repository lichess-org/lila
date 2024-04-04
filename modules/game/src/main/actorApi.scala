package lila.game
package actorApi

import chess.ByColor
import chess.format.Fen
import lila.rating.UserPerfs
import lila.core.user.User

case class StartGame(game: Game)

case class FinishGame(
    game: Game,
    // users and perfs BEFORE the game result is applied
    usersBeforeGame: ByColor[Option[(User, UserPerfs)]]
)

case class InsertGame(game: Game)

case class AbortedBy(pov: Pov)

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

case class PerfsUpdate(game: Game, perfs: ByColor[(lila.core.user.User, lila.rating.UserPerfs)])
