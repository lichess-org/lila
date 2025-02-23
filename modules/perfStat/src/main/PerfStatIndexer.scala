package lila.perfStat

import lila.rating.PerfType
import lila.rating.PerfType.GamePerf

final class PerfStatIndexer(
    gameRepo: lila.core.game.GameRepo,
    storage: PerfStatStorage
)(using Executor, Scheduler):

  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(64),
    timeout = 10.seconds,
    name = "perfStatIndexer",
    lila.log.asyncActorMonitor.full
  )

  private[perfStat] def userPerf(user: UserId, perf: GamePerf): Fu[PerfStat] =
    workQueue:
      storage
        .find(user, perf)
        .getOrElse(buildPerfStat(user, perf))

  private def buildPerfStat(user: UserId, perf: GamePerf): Fu[PerfStat] =
    gameRepo
      .sortedCursor(user.id, perf.key)
      .fold(PerfStat.init(user.id, perf)): (perfStat, game) =>
        if game.perfKey === perf.key
        then Pov(game, user.id).fold(perfStat)(perfStat.agg)
        else perfStat
      .flatMap: ps =>
        storage.insert(ps).recover(lila.db.ignoreDuplicateKey).inject(ps)
      .mon(_.perfStat.indexTime)

  def addGame(game: Game): Funit =
    game.players.toList.sequentiallyVoid: player =>
      player.userId.so: userId =>
        addPov(Pov(game, player), userId)

  private def addPov(pov: Pov, userId: UserId): Funit =
    PerfType
      .gamePerf(pov.game.perfKey)
      .so: (perf: GamePerf) =>
        storage
          .find(userId, perf)
          .flatMapz: perfStat =>
            storage.update(perfStat, perfStat.agg(pov))
