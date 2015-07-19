package lila.coach

import akka.actor._
import akka.pattern.ask

private[coach] final class Throttler(system: ActorSystem, f: String => Fu[UserStat]) {

  private implicit val timeout = makeTimeout.minutes(2)

  private val actor = system.actorOf(Props(new lila.hub.SequentialProvider {
    def process = {
      case id: String => f(id)
    }
  }))

  def apply(id: String): Fu[UserStat] = actor ? id mapTo manifest[UserStat]
}
