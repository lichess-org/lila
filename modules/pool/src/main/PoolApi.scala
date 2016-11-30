package lila.pool

import akka.actor._

import lila.user.User

final class PoolApi(
    val configs: List[PoolConfig],
    system: ActorSystem) {

  import PoolApi._
  import PoolActor._

  private val actors: Map[PoolConfig.Id, ActorRef] = configs.map { config =>
    config.id -> system.actorOf(
      Props(classOf[PoolActor], config),
      name = s"pool-${config.id.value}")
  }.toMap

  def join(poolId: PoolConfig.Id, joiner: Joiner) = actors foreach {
    case (id, actor) if id == poolId => actor ! Join(joiner)
    case (_, actor)                  => actor ! Leave(joiner.userId)
  }

  def leave(poolId: PoolConfig.Id, userId: User.ID) = sendTo(poolId, Leave(userId))

  private def sendTo(poolId: PoolConfig.Id, msg: Any) =
    actors get poolId foreach { _ ! msg }
}

object PoolApi {

  case class Joiner(userId: User.ID, ratingMap: Map[String, Int])
}
