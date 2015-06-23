package lila.relay

import akka.actor._
import akka.pattern.ask
import lila.hub.actorApi.map.Tell
import makeTimeout.veryLarge

final class RelayApi(
    fics: ActorRef,
    relayRepo: RelayRepo,
    actorMap: ActorRef,
    remote: java.net.InetSocketAddress) {

  def refreshFromFics: Funit = fics ? command.ListTourney mapTo
    manifest[command.ListTourney.Result] flatMap { tourneys =>
      tourneys.map { tourney =>
        relayRepo.upsert(tourney.id, tourney.name, tourney.status)
      }.sequenceFu.void >> relayRepo.started.flatMap {
        _.map { started =>
          (!tourneys.exists(_.name == started.name)) ?? relayRepo.finish(started)
        }.sequenceFu
      } >> relayRepo.started.flatMap(_.map(refreshGames).sequenceFu).void
    }

  private def refreshGames(relay: Relay): Funit =
    fics ? command.ListGames(relay.ficsId) mapTo
      manifest[command.ListGames.Result] flatMap { games =>
        val rgs = games.map { g =>
          relay gameByFicsId g.id match {
            case None     => Relay.Game make g.id
            case Some(rg) => rg
          }
        }
        val nr = relay.copy(games = rgs)
        println(s"[relay] ${nr.name}: ${nr.activeGames.size}/${nr.games.size} games")
        relayRepo.setGames(nr) >>-
          nr.activeGames.foreach { rg =>
            actorMap ! Tell(rg.ficsId.toString, GameActor.Recover)
          }
      }
}
