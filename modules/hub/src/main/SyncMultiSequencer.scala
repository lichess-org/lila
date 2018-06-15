package lila.hub

import akka.actor._
import scala.concurrent.Promise

/**
 * Sequences synchronous operation
 * balancing to round robin actors
 * for parallelism
 */
final class SyncMultiSequencer(
    system: ActorSystem,
    parallelismFactor: Int
) {

  def apply[A: Manifest](op: => A): Fu[A] = {
    val promise = Promise[A]()
    router ! Work(() => op, promise)
    promise.future
  }

  private case class Work[A](run: () => A, promise: Promise[A])

  private val router = system.actorOf(
    akka.routing.RoundRobinPool(parallelismFactor).props(Props(new Actor {
      def receive = {
        case Work(run, promise) => promise success run()
      }
    })), "api.round.router"
  )
}
