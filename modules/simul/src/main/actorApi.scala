package lila.simul
package actorApi

import lila.game.Game

private[simul] case class Talk(tourId: String, u: String, t: String, troll: Boolean)
private[simul] case class StartGame(game: Game, hostId: String)
private[simul] case class StartSimul(firstGame: Game, hostId: String)
private[simul] case class HostIsOn(gameId: GameId)
private[simul] case object Reload
private[simul] case object Aborted

private[simul] case object NotifyCrowd

private[simul] case class GetUserIdsP(promise: Promise[Iterable[UserId]])
