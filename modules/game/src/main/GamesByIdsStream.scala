package lila.game

import actorApi.{ FinishGame, StartGame }
import akka.stream.scaladsl._
import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.Bus
import lila.db.dsl._
import lila.game.Game

final class GamesByIdsStream(gameRepo: lila.game.GameRepo)(implicit
    mat: akka.stream.Materializer,
    ec: scala.concurrent.ExecutionContext
) {
  def apply(streamId: String, initialIds: Set[Game.ID], maxGames: Int): Source[JsValue, _] = {
    val startStream = Source.queue[Game](
      bufferSize = maxGames,
      akka.stream.OverflowStrategy.dropHead
    ) mapMaterializedValue { queue =>
      var watchedIds = initialIds
      val chans      = List("startGame", "finishGame", streamChan(streamId))
      val sub = Bus.subscribeFun(chans: _*) {
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
    gameSource(initialIds) concat startStream
  }
    .mapAsync(1)(gameRepo.withInitialFen)
    .map(GameStream.gameWithInitialFenWriter.writes)

  def addGameIds(streamId: String, gameIds: Set[String]) =
    Bus.publish(WatchGames(gameIds), streamChan(streamId))

  private case class WatchGames(ids: Set[Game.ID])
  private def streamChan(streamId: String) = s"gamesByIdsStream:$streamId"

  private def gameSource(ids: Set[Game.ID]) =
    if (ids.isEmpty) Source.empty[Game]
    else gameRepo.cursor($inIds(ids)).documentSource().throttle(50, 1.second)
}
