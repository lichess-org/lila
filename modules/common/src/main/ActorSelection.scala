package lila

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{ Future, Promise }
import scala.util.Success
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * TODO remove me when upgrading to akka 2.2.1
 */
trait LilaActorSelection {

  implicit final class lilaActorSelection(sel: akka.actor.ActorSelection) {

    /**
     * Resolve the [[ActorRef]] matching this selection.
     * The result is returned as a Future that is completed with the [[ActorRef]]
     * if such an actor exists. It is completed with failure [[ActorNotFound]] if
     * no such actor exists or the identification didn't complete within the
     * supplied `timeout`.
     *
     * Under the hood it talks to the actor to verify its existence and acquire its
     * [[ActorRef]].
     */
    def resolveOne()(implicit timeout: Timeout): Future[ActorRef] = {
      val p = Promise[ActorRef]()
      sel.ask(Identify(None)) onComplete {
        case Success(ActorIdentity(_, Some(ref))) ⇒ p.success(ref)
        case _                                    ⇒ p.failure(ActorNotFound(sel))
      }
      p.future
    }

    /**
     * Resolve the [[ActorRef]] matching this selection.
     * The result is returned as a Future that is completed with the [[ActorRef]]
     * if such an actor exists. It is completed with failure [[ActorNotFound]] if
     * no such actor exists or the identification didn't complete within the
     * supplied `timeout`.
     *
     * Under the hood it talks to the actor to verify its existence and acquire its
     * [[ActorRef]].
     */
    def resolveOne(timeout: FiniteDuration): Future[ActorRef] = resolveOne()(timeout)
  }
}
