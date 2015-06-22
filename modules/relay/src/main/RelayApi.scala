package lila.relay

import akka.actor._
import akka.pattern.ask
import makeTimeout.veryLarge

final class RelayApi(
    system: ActorSystem,
    relayRepo: RelayRepo,
    importer: Importer,
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
          games.map { g =>
            relay gameByFicsId g.id match {
              case None =>
                val rg = Relay.Game make g.id
                createGame(rg) inject rg
              case Some(rg) => fuccess(rg)
            }
          }.sequenceFu flatMap { relayRepo.setGames(relay, _) }
        }
    }.sequenceFu.void
  }

  def createGame(rg: Relay.Game): Funit = mainActor ? command.Moves(rg.ficsId) mapTo
    manifest[command.Moves.Result] flatMap importer(rg.id) void
}
