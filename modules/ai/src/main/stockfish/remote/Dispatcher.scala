package lila.ai
package stockfish
package remote

import scala.concurrent.duration._
import scala.util.Random

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout

import actorApi._
import lila.hub.actorApi.ai.GetLoad

private[ai] final class Dispatcher(
    urls: List[String],
    config: Config,
    router: String => Router) extends Actor {

  private var lastPlay = 0
  private var lastAnalysis = 0
  private var connectionsWithLoad: List[(ActorRef, Option[Int])] = Nil

  def receive = {

    case CalculateLoad => {
      implicit val timeout = makeTimeout(config.loadTimeout)
      connections map { c =>
        c ? GetLoad mapTo manifest[Option[Int]] map { l => List(c -> l) }
      }
    }.suml foreach { connectionsWithLoad = _ }

    case GetLoad => sender ! connectionsWithLoad.map(_._2)

    case x: Play => {
      val chosen = nextConnection(lastPlay)
      chosen foreach { c => lastPlay = c._1 }
      forward(chosen map (_._2), x, sender)(makeTimeout(config.playTimeout))
    }

    case x: Analyse => {
      implicit val timeout = makeTimeout(config.analyseTimeout)
      val chosen = nextConnection(lastAnalysis)
      chosen foreach { c => lastAnalysis = c._1 }
      forward(chosen map (_._2), x, sender)(makeTimeout(config.analyseTimeout))
    }
  }

  private def lessLoadedConnection: Option[ActorRef] =
    (connectionsWithLoad collect {
      case (a, Some(l)) => a -> (Random nextInt math.max(1, l))
    }).sortBy(x => x._2).headOption.map(_._1)

  private def nextConnection(lastUsed: Int): Option[(Int, ActorRef)] = {
    val (xs, nb, la) = (connectionsWithLoad, connections.size, lastUsed)
    val index = (la + 1) to (la + nb) map (_ % nb) find { index =>
      xs lift index exists (_._2.isDefined)
    }
    index flatMap { i =>
      xs lift i map (_._1) map { i -> _ }
    }
  }

  private def forward(to: Option[ActorRef], msg: Any, sender: ActorRef)(implicit timeout: Timeout) {
    to match {
      case None    => sender ! Status.Failure(new Exception("[stockfish dispatcher] No available remote found"))
      case Some(a) => a ? msg pipeTo sender
    }
  }

  private lazy val connections: List[ActorRef] = urls map { url =>
    val name = urlToActorName(url)
    context.actorOf(
      Props(new Connection(name, config, router(url))),
      name = name
    ) 
  }

  private val loadTick = context.system.scheduler.schedule(0.second, 1.second, self, CalculateLoad)

  override def postStop() {
    loadTick.cancel
  }

  private lazy val noRemote = new Exception("[stockfish dispatcher] No available remote found")

  private val urlRegex = """^https?://([^\/]+)/.+$""".r
  private def urlToActorName(url: String) = url match {
    case urlRegex(domain) => domain
    case _                => ornicar.scalalib.Random nextStringUppercase 8
  }
}
