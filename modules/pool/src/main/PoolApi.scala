package lila.pool

import akka.actor._

import lila.game.Game
import lila.rating.RatingRange
import lila.socket.Socket.{ Uid => SocketId }
import lila.user.User

final class PoolApi(
    val configs: List[PoolConfig],
    hookThieve: HookThieve,
    gameStarter: GameStarter,
    system: ActorSystem) {

  import PoolApi._
  import PoolActor._

  private val actors: Map[PoolConfig.Id, ActorRef] = configs.map { config =>
    config.id -> system.actorOf(
      Props(new PoolActor(config, hookThieve, gameStarter)),
      name = s"pool-${config.id.value}")
  }.toMap

  def join(poolId: PoolConfig.Id, joiner: Joiner) = actors foreach {
    case (id, actor) if id == poolId => actor ! Join(joiner)
    case (_, actor)                  => actor ! Leave(joiner.userId)
  }

  def leave(poolId: PoolConfig.Id, userId: User.ID) = sendTo(poolId, Leave(userId))

  def socketIds(ids: Set[String]) = {
    val msg = SocketIds(ids)
    actors.values.foreach(_ ! msg)
  }

  private def sendTo(poolId: PoolConfig.Id, msg: Any) =
    actors get poolId foreach { _ ! msg }
}

object PoolApi {

  case class Joiner(
      userId: User.ID,
      socketId: SocketId,
      ratingMap: Map[String, Int],
      ratingRange: Option[RatingRange],
      engine: Boolean,
      blocking: Set[String]) {

    def is(member: PoolMember) = userId == member.userId
  }

  case class Pairing(game: Game, whiteUid: SocketId, blackUid: SocketId)
}
