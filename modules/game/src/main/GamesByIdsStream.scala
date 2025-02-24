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
        val chans      = List("startGame", "finishGame", streamChan(streamId))
        val sub = Bus.subscribeFun(chans*):
          case lila.core.game.StartGame(game) if watchedIds(game.id) => queue.offer(game)
          case lila.core.game.FinishGame(game, _) if watchedIds(game.id) =>
            queue.offer(game)
            watchedIds = watchedIds - game.id
          case WatchGames(ids) =>
            val newIds = ids.diff(watchedIds)
            watchedIds = (watchedIds ++ newIds).take(maxGames)
            gameSource(newIds.intersect(watchedIds))
              .runWith(Sink.foreach(g => queue.offer(g)))
        queue
          .watchCompletion()
          .addEffectAnyway:
            Bus.unsubscribe(sub, chans)
    gameSource(initialIds)
      .concat(startStream)
      .mapAsync(1)(gameRepo.withInitialFen)
      .map(GameStream.gameWithInitialFenWriter.writes)

  def addGameIds(streamId: String, gameIds: Set[GameId]) =
    Bus.publish(WatchGames(gameIds), streamChan(streamId))

  private case class WatchGames(ids: Set[GameId])
  private def streamChan(streamId: String) = s"gamesByIdsStream:$streamId"

  private def gameSource(ids: Set[GameId]) =
    if ids.isEmpty then Source.empty[CoreGame]
    else gameRepo.cursor($inIds(ids)).documentSource().throttle(50, 1.second)
