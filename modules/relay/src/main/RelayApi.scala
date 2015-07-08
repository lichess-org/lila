package lila.relay

import akka.actor._
import akka.pattern.ask
import lila.common.paginator._
import lila.db.paginator.BSONAdapter
import lila.db.Types.Coll
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.map.Tell
import makeTimeout.veryLarge

final class RelayApi(
    coll: Coll,
    contentApi: ContentApi,
    fics: ActorRef,
    repo: RelayRepo,
    relayMap: ActorRef) {

  private[relay] def refreshFromFics: Funit = fics ? command.ListTourney mapTo
    manifest[command.ListTourney.Result] flatMap { tourneys =>
      tourneys.map { tourney =>
        repo.upsert(tourney.ficsId, tourney.name, tourney.status)
      }.sequenceFu.void >> repo.started.flatMap {
        _.map { started =>
          (!tourneys.exists(_.name == started.name)) ?? {
            repo.finish(started) >>- {
              relayMap ! Tell(started.id, lila.hub.SequentialActor.Terminate)
            }
          }
        }.sequenceFu
      }.void >>- repo.started.foreach {
        _ foreach { tourney =>
          relayMap ! Tell(tourney.id, TourneyActor.Recover)
        }
      }
    }

  private[relay] def setElo: Funit = repo.withGamesButNoElo flatMap {
    _.map { relay =>
      GameRepo games relay.gameIds flatMap { games =>
        games.flatMap(_.relay map (_.averageElo)).sorted.lastOption.?? {
          repo.setElo(relay.id, _)
        }
      }
    }.sequenceFu.void
  }

  def round(game: Game): Fu[Option[Relay.Round]] =
    game.relayId ?? repo.byId flatMap {
      _ ?? { relay =>
        GameRepo games relay.gameIds.filterNot(game.id ==) map {
          Relay.Round(relay, _).some
        }
      }
    }

  def paginator(page: Int): Fu[Paginator[Relay.WithContent]] = {
    import reactivemongo.bson._
    import BSONHandlers._
    Paginator(
      adapter = new BSONAdapter[Relay](
        collection = coll,
        selector = repo.selectNonEmpty,
        projection = BSONDocument(),
        sort = repo.sortRecent
      ) mapFutureList withContent,
      currentPage = page,
      maxPerPage = 30)
  }

  private def withContent(relays: Seq[Relay]): Fu[Seq[Relay.WithContent]] =
    contentApi byRelays relays map { contents =>
      relays map { r =>
        Relay.WithContent(r, contents.find(_ matches r))
      }
    }
}
