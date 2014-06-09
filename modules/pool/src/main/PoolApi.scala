package lila.pool

import akka.actor._
import akka.pattern.ask

import actorApi._
import lila.hub.actorApi.map.{ Ask }
import lila.user.User
import lila.game.{ GameRepo, Game }
import makeTimeout.short

final class PoolApi(hub: ActorRef) {

  def enter(pool: Pool, user: User): Funit = {
    hub ? Ask(pool.setup.id, Enter(user))
  }.void

  def leave(pool: Pool, user: User): Funit = {
    hub ? Ask(pool.setup.id, Leave(user.id))
  }.void

  def gamesOf(pool: Pool): Fu[List[Game]] =
    GameRepo games pool.pairings.take(4).map(_.gameId)
}
