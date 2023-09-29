package lila.perfStat

import lila.game.{ Game, GameRepo, Pov, Query }
import lila.rating.PerfType
import lila.user.User
import lila.common.config.Max

final class PerfStatIndexer(
    gameRepo: GameRepo,
    storage: PerfStatStorage
)(using Executor, Scheduler):

  private val workQueue =
    lila.hub.AsyncActorSequencer(maxSize = Max(64), timeout = 10 seconds, name = "perfStatIndexer")

  private[perfStat] def userPerf(user: User, perfType: PerfType): Fu[PerfStat] =
    workQueue:
      storage.find(user.id, perfType) getOrElse gameRepo
        .sortedCursor(
          Query.user(user.id) ++
            Query.finished ++
            Query.turnsGt(2) ++
            Query.variant(PerfType variantOf perfType),
          Query.sortChronological,
          readPref = _.priTemp
        )
        .fold(PerfStat.init(user.id, perfType)):
          case (perfStat, game) if game.perfType == perfType =>
            Pov(game, user.id).fold(perfStat)(perfStat.agg)
          case (perfStat, _) => perfStat
        .flatMap: ps =>
          storage insert ps recover lila.db.ignoreDuplicateKey inject ps
        .mon(_.perfStat.indexTime)

  def addGame(game: Game): Funit =
    game.players
      .flatMap: player =>
        player.userId.map: userId =>
          addPov(Pov(game, player), userId)
      .parallel
      .void

  private def addPov(pov: Pov, userId: UserId): Funit =
    storage
      .find(userId, pov.game.perfType)
      .flatMapz: perfStat =>
        storage.update(perfStat, perfStat agg pov)
