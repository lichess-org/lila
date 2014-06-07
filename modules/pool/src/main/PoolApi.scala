package lila.pool

import akka.actor._
import akka.pattern.ask

import lila.user.User
import makeTimeout.short
import lila.hub.actorApi.map.{ Ask }
import actorApi._

final class PoolApi(hub: ActorRef) {

  def enter(pool: Pool, user: User): Funit = {
    hub ? Ask(pool.setup.id, Enter(user))
  }.void

  def leave(pool: Pool, user: User): Funit = {
    hub ? Ask(pool.setup.id, Leave(user))
  }.void
}
