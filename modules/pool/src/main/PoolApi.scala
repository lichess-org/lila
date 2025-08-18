package lila.pool

import akka.actor.*

import lila.core.pool.{ PoolMember, PoolConfigId }
import lila.core.socket.Sris

final class PoolApi(
    val configs: List[PoolConfig],
    hookThieve: HookThieve,
    gameStarter: GameStarter,
    hasCurrentPlayban: lila.core.playban.HasCurrentPlayban,
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

  def join(poolId: PoolConfigId, member: PoolMember): Unit =
    hasCurrentPlayban(member.userId).foreach:
      case false =>
        actors.foreach: (id, actor) =>
          if id == poolId then
            rageSitOf(member.userId).foreach: rageSit =>
              actor ! Join(member.copy(rageSitCounter = rageSit.counterView))
          else actor ! Leave(member.userId)
      case _ =>

  def leave(poolId: PoolConfigId, userId: UserId) = sendTo(poolId, Leave(userId))

  def poolOf(clock: chess.Clock.Config): Option[PoolConfigId] =
    configs.find(_.clock == clock).map(_.id)

  def setOnlineSris(ids: Sris): Unit = actors.values.foreach(_ ! ids)

  private def sendTo(poolId: PoolConfigId, msg: Any) =
    actors.get(poolId).foreach { _ ! msg }
