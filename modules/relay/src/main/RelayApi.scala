package lila.relay

import akka.actor._
import akka.pattern.ask
import lila.hub.actorApi.map.Tell
import makeTimeout.veryLarge

final class RelayApi(
    fics: ActorRef,
    repo: RelayRepo,
    relayMap: ActorRef) {

  def refreshFromFics: Funit = fics ? command.ListTourney mapTo
    manifest[command.ListTourney.Result] flatMap { tourneys =>
      tourneys.map { tourney =>
        repo.upsert(tourney.id, tourney.name, tourney.status)
      }.sequenceFu.void >> repo.started.flatMap {
        _.map { started =>
          (!tourneys.exists(_.name == started.name)) ?? {
            repo.finish(started) >>- {
              relayMap ! Tell(started.id, PoisonPill)
            }
          }
        }.sequenceFu
      }.void >>- repo.started.foreach {
        _ foreach { tourney =>
          relayMap ! Tell(tourney.id, TourneyActor.Recover)
        }
      }
    }
}
