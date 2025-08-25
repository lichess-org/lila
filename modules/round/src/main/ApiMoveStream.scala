package lila.round

import akka.stream.scaladsl.*
import chess.format.Fen
import chess.{ ByColor, Centis, Ply, Position }
import play.api.libs.json.*

import lila.common.Bus
import lila.common.Json.given
import lila.core.game.FinishGame
import lila.game.GameRepo
import lila.game.actorApi.MoveGameEvent

final class ApiMoveStream(
    gameRepo: GameRepo,
    gameJsonView: lila.game.JsonView,
    lightUserApi: lila.user.LightUserApi
)(using Executor):

  def apply(game: Game, delayMoves: Boolean): Source[JsObject, ?] =
    Source.futureSource:
      for
        initialFen <- gameRepo.initialFen(game)
        lightUsers <- lightUserApi.asyncManyOptions(game.players.mapList(_.userId))
      yield
        val buffer = scala.collection.mutable.Queue.empty[JsObject]
        def makeGameJson(g: Game) =
          gameJsonView.baseWithChessDenorm(g, initialFen) ++ Json.obj(
            "players" -> JsObject(g.players.all.zip(lightUsers).map { (p, user) =>
              p.color.name -> gameJsonView.player(p, user)
            })
          )
        Source(List(makeGameJson(game))).concat(
          Source
            .queue[JsObject]((game.ply.value + 3).atLeast(16), akka.stream.OverflowStrategy.dropHead)
            .mapMaterializedValue: queue =>
              val clocks =
                for
                  clk <- game.clock
                  clkHistory <- game.clockHistory
                yield ByColor(
                  Vector(clk.config.initTime) ++ clkHistory.white,
                  Vector(clk.config.initTime) ++ clkHistory.black
                )
              val clockOffset = game.startColor.fold(0, 1)
              Position(game.variant, initialFen)
                .playPositions(game.sans)
                .foreach {
                  _.zipWithIndex.foreach: (s, index) =>
                    val clk = for
                      c <- clocks
                      white <- c.white.lift((index + 1 - clockOffset) >> 1)
                      black <- c.black.lift((index + clockOffset) >> 1)
                    yield ByColor(white, black)
                    queue.offer(
                      toJson(
                        Fen.write(s, (game.startedAtPly + index).fullMoveNumber),
                        s.history.lastMove.map(_.uci),
                        clk
                      )
                    )
                }
              if game.finished then
                queue.offer(makeGameJson(game))
                queue.complete()
              else
                val chan = MoveGameEvent.makeChan(game.id)
                val subEvent = Bus.subscribeFunDyn(chan):
                  case MoveGameEvent(g, fen, move) =>
                    queue.offer(toJson(g, fen, move.some))
                val subFinish = Bus.sub[FinishGame]:
                  case FinishGame(g, _) if g.id == game.id =>
                    queue.offer(makeGameJson(g))
                    (1 to buffer.size).foreach { _ => queue.offer(Json.obj()) } // push buffer content out
                    queue.complete()
                queue
                  .watchCompletion()
                  .addEffectAnyway:
                    Bus.unsubscribeDyn(subEvent, List(chan))
                    Bus.unsub[FinishGame](subFinish)
            .pipe: source =>
              if delayMoves && game.playable
              then source.delay(delayMovesBy(game), akka.stream.DelayOverflowStrategy.emitEarly)
              else source
        )
  end apply

  private def delayMovesBy(game: Game): FiniteDuration =
    game.clock
      .fold(60): clock =>
        (clock.config.estimateTotalSeconds / 60).atLeast(3).atMost(60)
      .seconds

  private def toJson(game: Game, fen: Fen.Full, lastMoveUci: Option[String]): JsObject =
    toJson(
      fen,
      lastMoveUci,
      game.clock.map: clk =>
        ByColor(clk.remainingTime)
    )

  private def toJson(fen: Fen.Full, lastMoveUci: Option[String], clock: Option[ByColor[Centis]]): JsObject =
    clock.foldLeft(
      Json
        .obj("fen" -> fen)
        .add("lm" -> lastMoveUci)
    ): (js, clk) =>
      js ++ Json.obj("wc" -> clk.white.roundSeconds, "bc" -> clk.black.roundSeconds)
