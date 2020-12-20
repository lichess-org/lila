package lila.game
package actorApi

import lila.user.User

case class StartGame(game: Game)

case class FinishGame(
    game: Game,
    white: Option[User],
    black: Option[User]
) {
  def isVsSelf = white.isDefined && white == black
}

case class InsertGame(game: Game)

case class AbortedBy(pov: Pov)

case class CorresAlarmEvent(pov: Pov)

private[game] case object NewCaptcha

case class MoveGameEvent(
    game: Game,
    fen: String,
    move: String
)
object MoveGameEvent {
  def makeChan(gameId: Game.ID) = s"moveEvent:$gameId"
}

case class BoardDrawOffer(pov: Pov)
