package lila.coach

import akka.actor._
import akka.pattern.ask

private[coach] final class Throttler(system: ActorSystem, f: String => Funit) {

  private implicit val timeout = makeTimeout.minutes(2)

  private val actor = system.actorOf(Props(new lila.hub.SequentialProvider {
    def process = {
      case id: String => f(id)
    }
  }))

  def apply(id: String): Funit = (actor ? id).void
}
