package lila.lobby

import akka.actor.*
import akka.stream.scaladsl.*
import play.api.libs.json.*
import scala.concurrent.duration.*

import lila.common.Bus

final class BoardApiHookStream(
    trouper: LobbySyncActor
)(using ec: scala.concurrent.ExecutionContext, system: ActorSystem):

  private case object SetOnline

  private val blueprint =
    Source.queue[Option[JsObject]](16, akka.stream.OverflowStrategy.dropHead)

  def apply(hook: Hook): Source[Option[JsObject], ?] =
    blueprint mapMaterializedValue { queue =>
      val actor = system.actorOf(Props(mkActor(hook, queue)))
      queue.watchCompletion().addEffectAnyway {
        actor ! PoisonPill
      }
    }

  private def mkActor(hook: Hook, queue: SourceQueueWithComplete[Option[JsObject]]) =
    new Actor:

      val classifiers = List(s"hookRemove:${hook.id}")

      override def preStart(): Unit =
        super.preStart()
        Bus.subscribe(self, classifiers)
        trouper ! actorApi.AddHook(hook)

      override def postStop() =
        super.postStop()
        Bus.unsubscribe(self, classifiers)
        trouper ! actorApi.CancelHook(hook.sri)
        queue.complete()

      self ! SetOnline

      def receive =

        case actorApi.RemoveHook(_) => self ! PoisonPill

        case SetOnline =>
          context.system.scheduler
            .scheduleOnce(3 second) {
              // gotta send a message to check if the client has disconnected
              queue offer None
              self ! SetOnline
            }
            .unit
