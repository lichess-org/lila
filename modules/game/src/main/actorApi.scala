package lila.game
package actorApi

import chess.format.Fen
import lila.user.User

case class StartGame(game: Game)

case class FinishGame(
    game: Game,
    white: Option[User],
    black: Option[User]
):
  def isVsSelf = white.isDefined && white == black

case class InsertGame(game: Game)

case class AbortedBy(pov: Pov)

case class CorresAlarmEvent(pov: Pov)

private[game] case object NewCaptcha

case class MoveGameEvent(
    game: Game,
    fen: Fen.Epd,
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
