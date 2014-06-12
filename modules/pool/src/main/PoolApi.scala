package lila.pool

import akka.actor._
import akka.pattern.ask

import actorApi._
import lila.game.{ GameRepo, Game }
import lila.hub.actorApi.map.{ Tell, Ask }
import lila.user.User
import makeTimeout.short

final class PoolApi(setupRepo: PoolSetupRepo, hub: ActorRef) {

  def enter(pool: Pool, user: User): Funit = {
    setupRepo.setups filterNot (_.id == pool.setup.id) foreach { setup =>
      hub ! Tell(setup.id, Leave(user.id))
    }
    hub ? Ask(pool.setup.id, Enter(user))
  }.void

  def leave(pool: Pool, user: User): Funit = {
    hub ? Ask(pool.setup.id, Leave(user.id))
  }.void

  def gamesOf(pool: Pool): Fu[List[Game]] =
    GameRepo games pool.pairings.take(4).map(_.gameId)
}
