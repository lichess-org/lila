package lila.ai
package stockfish
package remote

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._
import lila.hub.actorApi.ai.GetLoad

private[ai] final class Dispatcher(
    urls: List[String],
    router: String ⇒ Router,
    scheduler: Scheduler) extends Actor {

  private lazy val connections: List[ActorRef] = urls map { url ⇒
    context.actorOf(
      Props(new Connection(router(url))),
      name = urlToActorName(url)
    ) ~ { actor ⇒
        scheduler.schedule(200.millis, 1.second, actor, CalculateLoad)
      }
  }

  def receive = {
    case GetLoad    ⇒ loaded map { _ map { _._2 } } pipeTo sender
    case x: Play    ⇒ forward(x)
    case x: Analyse ⇒ forward(x)
  }

  private def forward(x: Any) = lessLoadedConnection.effectFold(
    e ⇒ sender ! Status.Failure(e),
    c ⇒ c forward x)

  private def loaded: Fu[List[(ActorRef, Option[Int])]] = {
    import makeTimeout.short
    connections map { c ⇒
      c ? GetLoad mapTo manifest[Option[Int]] map { l ⇒ List(c -> l) }
    }
  }.suml

  private def lessLoadedConnection: Fu[ActorRef] = loaded map { xs ⇒
    (xs collect {
      case (a, Some(l)) ⇒ a -> l
    }).sortBy(x ⇒ x._2).headOption.map(_._1)
  } flatten s"[stockfish] No available remote found"

  private val urlRegex = """^https?://([^\/]+)/.+$""".r
  private def urlToActorName(url: String) = url match {
    case urlRegex(domain) ⇒ domain
    case _                ⇒ ornicar.scalalib.Random nextString 8
  }
}
