package lila.pool

import akka.actor._

import lila.game.Game
import lila.rating.RatingRange
import lila.socket.Socket.{ Sri, Sris }
import lila.user.User

final class PoolApi(
    val configs: List[PoolConfig],
    hookThieve: HookThieve,
    gameStarter: GameStarter,
    playbanApi: lila.playban.PlaybanApi,
    system: ActorSystem
) {

  import PoolApi._
  import PoolActor._

  private val actors: Map[PoolConfig.Id, ActorRef] = configs.map { config =>
    config.id -> system.actorOf(
      Props(new PoolActor(config, hookThieve, gameStarter)),
      name = s"pool-${config.id.value}"
    )
  }.toMap

  def join(poolId: PoolConfig.Id, joiner: Joiner) =
    playbanApi.hasCurrentBan(joiner.userId) dforeach {
      case false =>
        actors foreach {
          case (id, actor) if id == poolId =>
            playbanApi.getRageSit(joiner.userId).dforeach(actor ! Join(joiner, _))
          case (_, actor) => actor ! Leave(joiner.userId)
        }
      case _ =>
    }

  def leave(poolId: PoolConfig.Id, userId: User.ID) = sendTo(poolId, Leave(userId))

  def socketIds(ids: Sris) = actors.values.foreach(_ ! ids)

  private def sendTo(poolId: PoolConfig.Id, msg: Any) =
    actors get poolId foreach { _ ! msg }
}

object PoolApi {

  case class Joiner(
      userId: User.ID,
      sri: Sri,
      ratingMap: Map[String, Int],
      ratingRange: Option[RatingRange],
      lame: Boolean,
      blocking: Set[String]
  ) {

    def is(member: PoolMember) = userId == member.userId
  }

  case class Pairing(game: Game, whiteSri: Sri, blackSri: Sri) {
    def sri(color: chess.Color) = color.fold(whiteSri, blackSri)
  }
  case class Pairings(pairings: List[Pairing])
}
