package lidraughts.tournament

import akka.actor._

import lidraughts.game.actorApi.FinishGame

private[tournament] final class ApiActor(api: TournamentApi) extends Actor {

  def receive = {

    case FinishGame(game, _, _) => api finishGame game

    case lidraughts.hub.actorApi.mod.MarkCheater(userId, true) => api ejectLame userId

    case lidraughts.hub.actorApi.mod.MarkBooster(userId) => api ejectLame userId

    case lidraughts.hub.actorApi.round.Berserk(gameId, userId) => api.berserk(gameId, userId)

    case lidraughts.hub.actorApi.playban.Playban(userId, _) => api.pausePlaybanned(userId)
  }
}
