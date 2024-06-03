package lila.perfStat

import lila.rating.PerfType

final class PerfStatIndexer(
    gameRepo: lila.core.game.GameRepo,
    storage: PerfStatStorage
)(using Executor, Scheduler):

  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(64),
    timeout = 10 seconds,
    name = "perfStatIndexer",
    lila.log.asyncActorMonitor
  )

  private[perfStat] def userPerf(user: UserId, perfKey: PerfKey): Fu[PerfStat] =
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
    storage
      .find(userId, pov.game.perfKey)
      .flatMapz: perfStat =>
        storage.update(perfStat, perfStat.agg(pov))
