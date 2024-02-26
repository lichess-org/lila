package lila.game
package actorApi

import lila.user.User

case class StartGame(game: Game)

case class FinishGame(
    game: Game,
    sente: Option[User],
    gote: Option[User]
) {
  def isVsSelf = sente.isDefined && sente == gote
}

case class PauseGame(game: Game)

case class InsertGame(game: Game)

case class AbortedBy(pov: Pov)

case class CorresAlarmEvent(pov: Pov)

private[game] case object NewCaptcha

case class MoveGameEvent(
    game: Game,
    sfen: String,
    move: String
)
object MoveGameEvent {
  def makeChan(gameId: Game.ID) = s"moveEvent:$gameId"
}

case class BoardDrawOffer(pov: Pov)
