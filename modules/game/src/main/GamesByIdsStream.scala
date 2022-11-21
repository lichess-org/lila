package lila.game

import actorApi.{ FinishGame, StartGame }
import akka.stream.scaladsl.*
import play.api.libs.json.*
import scala.concurrent.duration.*

import lila.common.Bus
import lila.db.dsl.*
import lila.game.Game
import lila.game.BSONHandlers.idHandler

final class GamesByIdsStream(gameRepo: lila.game.GameRepo)(using
    mat: akka.stream.Materializer,
    ec: scala.concurrent.ExecutionContext
):
  def apply(streamId: String, initialIds: Set[Game.Id], maxGames: Int): Source[JsValue, ?] =
    val startStream = Source.queue[Game](
      bufferSize = maxGames,
      akka.stream.OverflowStrategy.dropHead
    ) mapMaterializedValue { queue =>
      var watchedIds = initialIds
      val chans      = List("startGame", "finishGame", streamChan(streamId))
      val sub = Bus.subscribeFun(chans*) {
        case StartGame(game) if watchedIds(game.id) => queue.offer(game).unit
        case FinishGame(game, _, _) if watchedIds(game.id) =>
          queue.offer(game).unit
          watchedIds = watchedIds - game.id
        case WatchGames(ids) =>
          val newIds = ids diff watchedIds
          watchedIds = (watchedIds ++ newIds) take maxGames
          gameSource(newIds intersect watchedIds)
            .runWith(Sink.foreach(g => queue.offer(g).unit))
            .unit
      }
      queue.watchCompletion().foreach { _ =>
        Bus.unsubscribe(sub, chans)
      }
    }
    gameSource(initialIds)
      .concat(startStream)
      .mapAsync(1)(gameRepo.withInitialFen)
      .map(GameStream.gameWithInitialFenWriter.writes)

  def addGameIds(streamId: String, gameIds: Set[Game.Id]) =
    Bus.publish(WatchGames(gameIds), streamChan(streamId))

  private case class WatchGames(ids: Set[Game.Id])
  private def streamChan(streamId: String) = s"gamesByIdsStream:$streamId"

  private def gameSource(ids: Set[Game.Id]) =
    if (ids.isEmpty) Source.empty[Game]
    else gameRepo.cursor($inIds(ids)).documentSource().throttle(50, 1.second)
