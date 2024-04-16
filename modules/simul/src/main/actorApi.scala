package lila.simul
package actorApi

import lila.core.game.Game

private case class Talk(tourId: String, u: String, t: String, troll: Boolean)
private case class StartGame(game: Game, hostId: String)
private case class StartSimul(firstGame: Game, hostId: String)
private case class HostIsOn(gameId: GameId)
private case object Reload
private case object Aborted

private case object NotifyCrowd

private case class GetUserIdsP(promise: Promise[Iterable[UserId]])
