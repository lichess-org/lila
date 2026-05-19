package lila.game

import akka.stream.scaladsl.*
import play.api.libs.json.*

import lila.common.Bus
import lila.db.dsl.{ *, given }

final class GamesByIdsStream(gameRepo: lila.game.GameRepo)(using akka.stream.Materializer, Executor):

  def apply(streamId: String, initialIds: Set[GameId], maxGames: Int): Source[JsValue, ?] =
    val startStream = Source
      .queue[CoreGame](
        bufferSize = maxGames,
        akka.stream.OverflowStrategy.dropHead
      )
      .mapMaterializedValue: queue =>
        var watchedIds = initialIds
        val subStart = Bus.sub[lila.core.game.StartGame]:
          case lila.core.game.StartGame(game, _) if watchedIds(game.id) => queue.offer(game)
        val subFinish = Bus.sub[lila.core.game.FinishGame]:
          case lila.core.game.FinishGame(game, _) if watchedIds(game.id) =>
            queue.offer(game)
            watchedIds = watchedIds - game.id
        val subWatch = Bus.subscribeFunDyn(streamChan(streamId)):
          case WatchGames(ids) =>
            val newIds = ids.diff(watchedIds)
            watchedIds = (watchedIds ++ newIds).take(maxGames)
            gameSource(newIds.intersect(watchedIds))
              .runWith(Sink.foreach(g => queue.offer(g)))
        queue
          .watchCompletion()
          .addEffectAnyway:
            Bus.unsub[lila.core.game.StartGame](subStart)
            Bus.unsub[lila.core.game.FinishGame](subFinish)
            Bus.unsubscribeDyn(subWatch, List(streamChan(streamId)))
    gameSource(initialIds)
      .concat(startStream)
      .mapAsync(1)(gameRepo.withInitialFen)
      .map(GameStream.toJson(none))

  def addGameIds(streamId: String, gameIds: Set[GameId]) =
    Bus.publishDyn(WatchGames(gameIds), streamChan(streamId))

  def exists(streamId: String): Boolean = Bus.exists(streamChan(streamId))

  private case class WatchGames(ids: Set[GameId])
  private def streamChan(streamId: String) = s"gamesByIdsStream:$streamId"

  private def gameSource(ids: Set[GameId]) =
    if ids.isEmpty then Source.empty[CoreGame]
    else gameRepo.cursor($inIds(ids)).documentSource().throttle(50, 1.second)
