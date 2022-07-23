package lila.hub

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.log.Logger

/** Runs the work then waits cooldown only runs once at a time per id. Guarantees that work is ran as early as
  * possible. Also saves work and runs it after cooldown.
  */
final class EarlyMultiThrottler(logger: Logger)(implicit ec: ExecutionContext, system: ActorSystem) {

  private val actor = system.actorOf(Props(new EarlyMultiThrottlerActor(logger)))

  def apply(id: String, cooldown: FiniteDuration)(run: => Funit) =
    actor ! EarlyMultiThrottlerActor.Work(id, run = () => run, cooldown)
}

// actor based implementation
final private class EarlyMultiThrottlerActor(logger: Logger)(implicit ec: ExecutionContext) extends Actor {

  import EarlyMultiThrottlerActor._

  var running = Set.empty[String]
  var planned = Map.empty[String, Work]

  def receive: Receive = {

    case work: Work if !running(work.id) =>
      execute(work) addEffectAnyway {
        self ! Done(work.id)
      }
      running = running + work.id

    case work: Work => // already executing similar work
      planned = planned + (work.id -> work)

    case Done(id) =>
      running = running - id
      planned get id foreach { work =>
        self ! work
        planned = planned - work.id
      }

    case x => logger.branch("EarlyMultiThrottler").warn(s"Unsupported message $x")
  }

  implicit def scheduler = context.system.scheduler

  def execute(work: Work): Funit =
    lila.common.Future.makeItLast(work.cooldown) { work.run() }
}

private object EarlyMultiThrottlerActor {
  case class Work(
      id: String,
      run: () => Funit,
      cooldown: FiniteDuration // how long to wait after running, before next run
  )
  private case class Done(id: String)
}
