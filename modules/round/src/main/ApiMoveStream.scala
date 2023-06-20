package lila.round

import akka.stream.scaladsl.*
import chess.format.Fen
import chess.{ Centis, Replay, ByColor }
import play.api.libs.json.*
import scala.util.chaining.*

import lila.common.Bus
import lila.common.Json.given
import lila.game.actorApi.{ FinishGame, MoveGameEvent }
import lila.game.{ Game, GameRepo }
import chess.{ Ply, Situation }

final class ApiMoveStream(
    gameRepo: GameRepo,
    gameJsonView: lila.game.JsonView,
    lightUserApi: lila.user.LightUserApi
)(using Executor):

  def apply(game: Game, delayMoves: Boolean): Source[JsObject, ?] =
    Source.futureSource:
      for
        initialFen <- gameRepo.initialFen(game)
        lightUsers <- lightUserApi.asyncManyOptions(game.players.map(_.userId))
      yield
        val buffer = scala.collection.mutable.Queue.empty[JsObject]
        def makeGameJson(g: Game) =
          gameJsonView.baseWithChessDenorm(g, initialFen) ++ Json.obj(
            "players" -> JsObject(g.players zip lightUsers map { (p, user) =>
              p.color.name -> gameJsonView.player(p, user)
            })
          )
        Source(List(makeGameJson(game))) concat
          Source
            .queue[JsObject]((game.ply.value + 3) atLeast 16, akka.stream.OverflowStrategy.dropHead)
            .mapMaterializedValue: queue =>
              val clocks = for
                clk        <- game.clock
                clkHistory <- game.clockHistory
              yield ByColor(
                Vector(clk.config.initTime) ++ clkHistory.white,
                Vector(clk.config.initTime) ++ clkHistory.black
              )
              val clockOffset = game.startColor.fold(0, 1)
              Replay.situations(game.sans, initialFen, game.variant) foreach {
                _.zipWithIndex.foreach: (s, index) =>
                  val clk = for
                    c     <- clocks
                    white <- c.white.lift((index + 1 - clockOffset) >> 1)
                    black <- c.black.lift((index + clockOffset) >> 1)
                  yield ByColor(white, black)
                  queue offer toJson(
                    Fen write Situation
                      .AndFullMoveNumber(s, (game.startedAtPly + index).fullMoveNumber),
                    s.board.history.lastMove.map(_.uci),
                    clk
                  )
              }
              if game.finished then
                queue offer makeGameJson(game)
                queue.complete()
              else
                val chans = List(MoveGameEvent makeChan game.id, "finishGame")
                val sub = Bus.subscribeFun(chans*):
                  case MoveGameEvent(g, fen, move) =>
                    queue.offer(toJson(g, fen, move.some)).unit
                  case FinishGame(g, _, _) if g.id == game.id =>
                    queue offer makeGameJson(g)
                    (1 to buffer.size) foreach { _ => queue.offer(Json.obj()) } // push buffer content out
                    queue.complete()
                queue
                  .watchCompletion()
                  .addEffectAnyway:
                    Bus.unsubscribe(sub, chans)
            .pipe: source =>
              if delayMoves
              then source.delay(delayMovesBy(game), akka.stream.DelayOverflowStrategy.emitEarly)
              else source
  end apply

  private def delayMovesBy(game: Game): FiniteDuration =
    game.clock
      .fold(60): clock =>
        (clock.config.estimateTotalSeconds / 60) atLeast 3 atMost 60
      .seconds

  private def toJson(game: Game, fen: Fen.Epd, lastMoveUci: Option[String]): JsObject =
    toJson(
      fen,
      lastMoveUci,
      game.clock.map: clk =>
        ByColor(clk.remainingTime)
    )

  private def toJson(fen: Fen.Epd, lastMoveUci: Option[String], clock: Option[ByColor[Centis]]): JsObject =
    clock.foldLeft(
      Json
        .obj("fen" -> fen)
        .add("lm" -> lastMoveUci)
    ): (js, clk) =>
      js ++ Json.obj("wc" -> clk.white.roundSeconds, "bc" -> clk.black.roundSeconds)
