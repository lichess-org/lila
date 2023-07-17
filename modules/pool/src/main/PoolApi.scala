package lila.pool

import akka.actor.*

import lila.game.Game
import lila.rating.{ PerfType, RatingRange }
import lila.socket.Socket.{ Sri, Sris }
import lila.user.Me

final class PoolApi(
    val configs: List[PoolConfig],
    hookThieve: HookThieve,
    gameStarter: GameStarter,
    playbanApi: lila.playban.PlaybanApi,
    system: ActorSystem
)(using Executor):
  import PoolApi.*
  import PoolActor.*

  private val actors: Map[PoolConfig.Id, ActorRef] = configs
    .map: config =>
      config.id -> system
        .actorOf(
          Props(PoolActor(config, hookThieve, gameStarter)),
          name = s"pool-${config.id}"
        )
    .toMap

  val poolPerfTypes: Map[PoolConfig.Id, PerfType] = configs
    .map: config =>
      config.id -> config.perfType
    .toMap

  def join(poolId: PoolConfig.Id, joiner: Joiner): Unit =
    playbanApi
      .hasCurrentBan(joiner)
      .foreach:
        case false =>
          actors.foreach:
            case (id, actor) if id == poolId =>
              playbanApi.getRageSit(joiner.me).foreach(actor ! Join(joiner, _))
            case (_, actor) => actor ! Leave(joiner.me)
        case _ =>

  def leave(poolId: PoolConfig.Id, userId: UserId) = sendTo(poolId, Leave(userId))

  def socketIds(ids: Sris) = actors.values.foreach(_ ! ids)

  private def sendTo(poolId: PoolConfig.Id, msg: Any) =
    actors get poolId foreach { _ ! msg }

object PoolApi:

  case class Joiner(
      sri: Sri,
      rating: IntRating,
      ratingRange: Option[RatingRange],
      lame: Boolean,
      blocking: Blocking
  )(using val me: Me.Id):
    def is(member: PoolMember) = member is me

  object Joiner:
    given UserIdOf[Joiner] = _.me.userId

  case class Pairing(game: Game, whiteSri: Sri, blackSri: Sri):
    def sri(color: chess.Color) = color.fold(whiteSri, blackSri)
  case class Pairings(pairings: List[Pairing])
