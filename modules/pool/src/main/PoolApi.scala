package lila.pool

import akka.actor.*

import lila.core.pool.{ Joiner, PoolConfigId }
import lila.core.socket.Sris

final class PoolApi(
    val configs: List[PoolConfig],
    hookThieve: HookThieve,
    gameStarter: GameStarter,
    HasCurrentPlayban: lila.core.playban.HasCurrentPlayban,
    rageSitOf: lila.core.playban.RageSitOf,
    system: ActorSystem
)(using Executor)
    extends lila.core.pool.PoolApi:
  import PoolActor.*

  private val actors: Map[PoolConfigId, ActorRef] = configs
    .map: config =>
      config.id -> system
        .actorOf(
          Props(PoolActor(config, hookThieve, gameStarter)),
          name = s"pool-${config.id}"
        )
    .toMap

  val poolPerfKeys: Map[PoolConfigId, PerfKey] = configs
    .map: config =>
      config.id -> config.perfKey
    .toMap

  def join(poolId: PoolConfigId, joiner: Joiner): Unit =
    HasCurrentPlayban(joiner.me.id).foreach:
      case false =>
        actors.foreach:
          case (id, actor) if id == poolId =>
            rageSitOf(joiner.me.id).foreach(actor ! Join(joiner, _))
          case (_, actor) => actor ! Leave(joiner.me)
      case _ =>

  def leave(poolId: PoolConfigId, userId: UserId) = sendTo(poolId, Leave(userId))

  def setOnlineSris(ids: Sris): Unit = actors.values.foreach(_ ! ids)

  private def sendTo(poolId: PoolConfigId, msg: Any) =
    actors.get(poolId).foreach { _ ! msg }
