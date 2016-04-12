package lila.tournament

import scala.concurrent.duration._
import akka.actor._

import lila.game.actorApi.FinishGame

private[tournament] final class ApiActor(api: TournamentApi) extends Actor {

  override def preStart {
  }

  def receive = {

    case FinishGame(game, _, _)                          => api finishGame game

    case lila.hub.actorApi.mod.MarkCheater(userId)       => api ejectLame userId

    case lila.hub.actorApi.mod.MarkBooster(userId)       => api ejectLame userId

    case lila.hub.actorApi.round.Berserk(gameId, userId) => api.berserk(gameId, userId)
  }
}
