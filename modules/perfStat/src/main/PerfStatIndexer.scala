package lila.perfStat

import lila.game.{ GameRepo, Query }
import lila.rating.PerfType

final class PerfStatIndexer(
    gameRepo: GameRepo,
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
            .sortedCursor(
              Query.user(user.id) ++
                Query.finished ++
                Query.turnsGt(2) ++
                Query.variant(lila.rating.PerfType.variantOf(perfKey)),
              Query.sortChronological,
              readPref = _.priTemp
            )
            .fold(PerfStat.init(user.id, perfKey)):
              case (perfStat, game) if game.perfKey == perfKey =>
                Pov(game, user.id).fold(perfStat)(perfStat.agg)
              case (perfStat, _) => perfStat
            .flatMap: ps =>
              storage.insert(ps).recover(lila.db.ignoreDuplicateKey).inject(ps)
            .mon(_.perfStat.indexTime)
        )

  def addGame(game: Game): Funit =
    game.players
      .flatMap: player =>
        player.userId.map: userId =>
          addPov(Pov(game, player), userId)
      .parallel
      .void

  private def addPov(pov: Pov, userId: UserId): Funit =
    storage
      .find(userId, pov.game.perfKey)
      .flatMapz: perfStat =>
        storage.update(perfStat, perfStat.agg(pov))
