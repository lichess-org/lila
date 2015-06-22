package lila.relay

import akka.actor._
import akka.pattern.ask
import makeTimeout.veryLarge

final class RelayApi(
    system: ActorSystem,
    relayRepo: RelayRepo,
    remote: java.net.InetSocketAddress) {

  private val mainActor = system.actorOf(Props(classOf[FicsActor], remote))

  def refreshRelays: Funit = mainActor ? command.ListTourney mapTo
    manifest[command.ListTourney.Result] flatMap {
      _.map { tourney =>
        relayRepo.upsert(tourney.id, tourney.name, tourney.status)
      }.sequenceFu.void
    }

  def refreshRelayGames: Funit = relayRepo.started.flatMap {
    _.map { relay =>
      mainActor ? command.ListGames(relay.ficsId) mapTo
        manifest[command.ListGames.Result] flatMap { games =>
          val relayGames = games.map { g =>
            relay gameByFicsId g.id match {
              case None     => RelayGame.make(g.id, g.white, g.black)
              case Some(rg) => rg
            }
          }
          relayRepo.setGames(relay, relayGames)
        }
    }.sequenceFu.void
  }
}
