package lila.lobby

import akka.actor.*
import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Bus
import lila.common.actorBus.*
import lila.core.socket.Sri

final class BoardApiHookStream(
    lobby: LobbySyncActor
)(using ec: Executor, system: ActorSystem):

  private case object SetOnline

  private val blueprint =
    Source.queue[Option[JsObject]](16, akka.stream.OverflowStrategy.dropHead)

  def apply(hook: Hook): Source[Option[JsObject], ?] =
    blueprint.mapMaterializedValue: queue =>
      val actor = system.actorOf(Props(mkActor(hook, queue)))
      queue
        .watchCompletion()
        .addEffectAnyway:
          actor ! PoisonPill

  def cancel(sri: Sri) = Bus.publish(RemoveHook(sri.value), s"hookRemove:${sri}")

  private def mkActor(hook: Hook, queue: SourceQueueWithComplete[Option[JsObject]]): Actor = new:

    val classifiers = List(s"hookRemove:${hook.id}", s"hookRemove:${hook.sri}")

    override def preStart(): Unit =
      super.preStart()
      Bus.subscribe(self, classifiers)
      lobby ! AddHook(hook)

    override def postStop() =
      super.postStop()
      classifiers.foreach(Bus.unsubscribe(self, _))
      lobby ! CancelHook(hook.sri)
      queue.complete()

    self ! SetOnline

    def receive =

      case RemoveHook(_) => self ! PoisonPill

      case SetOnline =>
        context.system.scheduler
          .scheduleOnce(3.second):
            // gotta send a message to check if the client has disconnected
            queue.offer(None)
            self ! SetOnline
