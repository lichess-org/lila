package lila.lobby

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Bus
import lila.core.socket.Sri

final class BoardApiHookStream(
    lobby: LobbySyncActor,
    userApi: lila.core.user.UserApi
)(using scheduler: Scheduler)(using Executor):

  private val blueprint =
    Source.queue[Option[JsObject]](16, akka.stream.OverflowStrategy.dropHead)

  def apply(hook: Hook): Source[Option[JsObject], ?] =

    val classifiers = List(s"hookRemove:${hook.id}", s"hookRemove:${hook.sri}")

    blueprint.mapMaterializedValue: queue =>

      val busHandler = scalalib.bus.Tellable:
        case RemoveHook(_) => queue.complete()

      Bus.subscribeDyn(busHandler, classifiers*)

      val periodicBlankLine = scheduler.scheduleWithFixedDelay(10.seconds, 10.seconds): () =>
        queue.offer(None)

      queue
        .watchCompletion()
        .addEffectAnyway:
          Bus.unsubscribeDyn(busHandler, classifiers)
          lobby ! CancelHook(hook.sri)
          periodicBlankLine.cancel()

      lobby ! SetupBus.AddHook(hook)

  def cancel(sri: Sri) = Bus.publishDyn(RemoveHook(sri.value), s"hookRemove:${sri}")

  def mustPlayAsColor(chosen: TriColor)(using me: Option[Me]): Fu[Option[Color]] =
    (chosen != TriColor.Random).so:
      me.map(_.userId).so(userApi.mustPlayAsColor).map(_.filter(_ != chosen.resolve()))
