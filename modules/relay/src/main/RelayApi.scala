package lila.relay

import akka.actor._
import akka.pattern.ask
import lila.hub.actorApi.map.Tell
import makeTimeout.veryLarge

final class RelayApi(
    system: ActorSystem,
    relayRepo: RelayRepo,
    actorMap: ActorRef,
    remote: java.net.InetSocketAddress) {

  private val fics = system.actorOf(Props(classOf[FicsActor], actorMap, remote))

  def refreshRelays: Funit = fics ? command.ListTourney mapTo
    manifest[command.ListTourney.Result] flatMap {
      _.map { tourney =>
        relayRepo.upsert(tourney.id, tourney.name, tourney.status)
      }.sequenceFu.void
    }

  def refreshRelayGames: Funit = relayRepo.started.flatMap {
    _.map { relay =>
      fics ? command.ListGames(relay.ficsId) mapTo
        manifest[command.ListGames.Result] flatMap { games =>
          games.map { g =>
            relay gameByFicsId g.id match {
              case None =>
                val rg = Relay.Game make g.id
                createGame(rg) inject rg
              case Some(rg) => fuccess(rg)
            }
          }.sequenceFu flatMap { rgs =>
            relayRepo.setGames(relay, rgs) >>-
              rgs.foreach { rg => fics ! FicsActor.Observe(rg.ficsId) }
          }
        }
    }.sequenceFu.void
  }

  def createGame(rg: Relay.Game): Funit = fics ? command.Moves(rg.ficsId) mapTo
    manifest[command.Moves.Result] map { game =>
      actorMap ! Tell(rg.ficsId.toString, GameActor.Import(rg.id, game))
    }
}
