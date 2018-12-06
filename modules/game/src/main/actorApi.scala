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

case class SimulNextGame(hostId: String, game: Game)

private[game] case object NewCaptcha

case class MoveGameEvent(
    game: Game,
    fen: String,
    move: String
)
object MoveGameEvent {
  def makeSymbol(gameId: Game.ID) = Symbol(s"moveEvent:$gameId")
  def makeBusEvent(event: MoveGameEvent) = lidraughts.common.Bus.Event(event, makeSymbol(event.game.id))
}
