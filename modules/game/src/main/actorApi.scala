package lidraughts.game
package actorApi

import lidraughts.user.User

case class StartGame(game: Game)
case class UserStartGame(userId: String, game: Game)

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
