package lila.insight

import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.common.LilaStream
import lila.db.dsl.{ *, given }
import lila.game.{ GameRepo, Query }

final private class InsightIndexer(
    povToEntry: PovToEntry,
    gameRepo: GameRepo,
    storage: InsightStorage
)(using Executor, Scheduler, akka.stream.Materializer):

  import gameRepo.gameHandler

  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(256),
    timeout = 1.minute,
    name = "insightIndexer",
    lila.log.asyncActorMonitor.full
  )

  def all(user: User, force: Boolean): Funit =
    (force || (user.lame.not && user.noBot)).so:
      workQueue:
        storage
          .fetchLast(user.id)
          .flatMap:
            _.fold(fromScratch(user)): e =>
              computeFrom(user, e.date.plusSeconds(1))

  def update(game: Game, userId: UserId, previous: InsightEntry): Funit =
    povToEntry(game, userId, previous.provisional).flatMap:
      case Right(e) => storage.update(e)
      case _ => funit

  private def fromScratch(user: User): Funit =
    fetchFirstGame(user).flatMapz: g =>
      computeFrom(user, g.createdAt)

  private def gameQuery(user: User) =
    Query.user(user.id) ++
      Query.rated ++
      Query.finished ++
      Query.turnsGt(2) ++
      Query.notFromPosition ++
      Query.notHordeOrSincePawnsAreWhite

  private def fetchFirstGame(user: User): Fu[Option[Game]] =
    if user.count.rated == 0 then fuccess(none)
    else
      (maxGames < user.count.rated)
        .so:
          gameRepo.coll
            .find(gameQuery(user))
            .sort(Query.sortCreated)
            .skip(maxGames.value - 1)
            .one[Game](ReadPref.sec)
        .orElse:
          gameRepo.coll
            .find(gameQuery(user))
            .sort(Query.sortChronological)
            .one[Game](ReadPref.sec)

  private def computeFrom(user: User, from: Instant): Funit =
    storage
      .nbByPerf(user.id)
      .flatMap: nbs =>
        var nbByPerf = nbs
        def toEntry(game: Game): Fu[Option[InsightEntry]] =
          val nb = nbByPerf.getOrElse(game.perfKey, 0) + 1
          nbByPerf = nbByPerf.updated(game.perfKey, nb)
          povToEntry(game, user.id, provisional = nb < 10)
            .addFailureEffect: e =>
              logger.warn(e.getMessage, e)
            .map(_.toOption)
        val query = gameQuery(user) ++ $doc(lila.game.Game.BSONFields.createdAt.$gte(from))
        gameRepo
          .sortedCursor(query, Query.sortChronological)
          .documentSource(maxGames.value)
          .mapAsync(16)(toEntry)
          .via(LilaStream.collect)
          .grouped(100.atMost(maxGames.value))
          .map(storage.bulkInsert)
          .run()
          .void
