package lila.perfStat

import lila.rating.PerfType
import lila.rating.PerfType.GamePerf

final class PerfStatIndexer(
    gameRepo: lila.core.game.GameRepo,
    storage: PerfStatStorage
)(using Executor, Scheduler):

  import PerfType.{ isLeaderboardable as isRelevant }

  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(64),
    timeout = 10 seconds,
    name = "perfStatIndexer",
    lila.log.asyncActorMonitor.full
  )

  private[perfStat] def userPerf(user: UserId, perfKey: GamePerf): Fu[PerfStat] =
    workQueue:
      storage
        .find(user, perfKey)
        .getOrElse(
          gameRepo
            .sortedCursor(user.id, perfKey)
            .fold(PerfStat.init(user.id, perfKey)):
              case (perfStat, game) if game.perfKey == perfKey =>
                Pov(game, user.id).fold(perfStat)(perfStat.agg)
              case (perfStat, _) => perfStat
            .flatMap: ps =>
              storage.insert(ps).recover(lila.db.ignoreDuplicateKey).inject(ps)
            .mon(_.perfStat.indexTime)
        )

  def addGame(game: Game): Funit =
    game.players.toList.sequentiallyVoid: player =>
      player.userId.so: userId =>
        addPov(Pov(game, player), userId)

  private def addPov(pov: Pov, userId: UserId): Funit =
    PerfType
      .gamePerf(pov.game.perfKey)
      .so: (pk: GamePerf) =>
        storage
          .find(userId, pk)
          .flatMapz: perfStat =>
            storage.update(perfStat, perfStat.agg(pov))
