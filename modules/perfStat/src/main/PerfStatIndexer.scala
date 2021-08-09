package lila.perfStat

import scala.concurrent.duration._
import reactivemongo.api.ReadPreference

import lila.game.{ Game, GameRepo, Pov, Query }
import lila.rating.PerfType
import lila.user.User

final class PerfStatIndexer(
    gameRepo: GameRepo,
    storage: PerfStatStorage
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  private val workQueue =
    new lila.hub.AsyncActorSequencer(maxSize = 64, timeout = 10 seconds, name = "perfStatIndexer")

  private[perfStat] def userPerf(user: User, perfType: PerfType): Fu[PerfStat] =
    workQueue {
      storage.find(user.id, perfType) getOrElse gameRepo
        .sortedCursor(
          Query.user(user.id) ++
            Query.finished ++
            Query.turnsGt(2) ++
            Query.variant(PerfType variantOf perfType),
          Query.sortChronological,
          readPreference = ReadPreference.secondaryPreferred
        )
        .fold(PerfStat.init(user.id, perfType)) {
          case (perfStat, game) if game.perfType.contains(perfType) =>
            Pov.ofUserId(game, user.id).fold(perfStat)(perfStat.agg)
          case (perfStat, _) => perfStat
        }
        .flatMap { ps =>
          storage insert ps recover lila.db.recoverDuplicateKey(_ => ()) inject ps
        }
        .mon(_.perfStat.indexTime)
    }

  def addGame(game: Game): Funit =
    game.players
      .flatMap { player =>
        player.userId.map { userId =>
          addPov(Pov(game, player), userId)
        }
      }
      .sequenceFu
      .void

  private def addPov(pov: Pov, userId: String): Funit =
    pov.game.perfType ?? { perfType =>
      storage.find(userId, perfType) flatMap {
        _ ?? { perfStat =>
          storage.update(perfStat, perfStat agg pov)
        }
      }
    }
}
