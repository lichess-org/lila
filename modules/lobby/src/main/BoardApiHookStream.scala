package lila.lobby

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Bus
import lila.core.socket.Sri
import akka.actor.Cancellable
import lila.core.pool.PoolFrom

final class BoardApiHookStream(
    lobby: LobbySyncActor,
    userApi: lila.core.user.UserApi,
    poolApi: lila.core.pool.PoolApi
)(using scheduler: Scheduler)(using Executor):

  private val blueprint =
    Source.queue[Option[JsObject]](4, akka.stream.OverflowStrategy.dropHead)

  def apply(hook: Hook): Source[Option[JsObject], ?] = asPool(hook) | asHook(hook)

  private def asHook(hook: Hook): Source[Option[JsObject], ?] =

    val channels = List(s"hookRemove:${hook.id}", s"hookRemove:${hook.sri}")

    blueprint.mapMaterializedValue: queue =>

      val busHandler = scalalib.bus.Tellable:
        case RemoveHook(_) => queue.complete()

      Bus.subscribeDyn(busHandler, channels*)

      val keepAlive = periodicBlankLine(queue)

      queue
        .watchCompletion()
        .addEffectAnyway:
          keepAlive.cancel()
          Bus.unsubscribeDyn(busHandler, channels)
          lobby ! CancelHook(hook.sri)

      lobby ! SetupBus.AddHook(hook)

  private def asPool(hook: Hook): Option[Source[Option[JsObject], ?]] = for
    poolId <- poolApi.poolOf(hook.clock)
    if hook.seemsCompatibleWithPools
    member <- Hook.asPoolMember(hook, PoolFrom.Api)
  yield
    val channel = s"hookRemove:${member.sri}"

    blueprint.mapMaterializedValue: queue =>

      val busHandler = scalalib.bus.Tellable:
        // lets the mobile withdraw from pool with `DELETE /api/board/seek
        case RemoveHook(_) => queue.complete()
        case _: lila.core.pool.Pairing => queue.complete()

      Bus.subscribeDyn(busHandler, channel)

      val keepAlive = periodicBlankLine(queue)

      queue
        .watchCompletion()
        .addEffectAnyway:
          keepAlive.cancel()
          Bus.unsubscribeDyn(busHandler, List(channel))
          poolApi.leave(poolId, member.userId)

      poolApi.join(poolId, member)

  private def periodicBlankLine(queue: SourceQueue[Option[JsObject]]): Cancellable =
    scheduler.scheduleWithFixedDelay(10.seconds, 10.seconds): () =>
      queue.offer(None)

  def cancel(sri: Sri) = Bus.publishDyn(RemoveHook(sri.value), s"hookRemove:${sri}")

  def mustPlayAsColor(chosen: TriColor)(using me: Option[Me]): Fu[Option[Color]] =
    (chosen != TriColor.Random).so:
      me.map(_.userId).so(userApi.mustPlayAsColor).map(_.filter(_ != chosen.resolve()))
