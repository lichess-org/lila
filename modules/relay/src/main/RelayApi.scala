package lila.relay

import akka.actor._
import akka.pattern.ask
import makeTimeout.veryLarge

final class RelayApi(
    system: ActorSystem,
    relayRepo: RelayRepo,
    remote: java.net.InetSocketAddress) {

  def refresh: Funit = {
    val actor = system.actorOf(Props(classOf[FicsActor], remote))
    actor ? command.ListTourney mapTo
      manifest[command.ListTourney.Result] flatMap {
        _.map { tourney =>
          relayRepo.upsert(tourney.id, tourney.name, tourney.status)
        }.sequenceFu.void
      }
  }
}
