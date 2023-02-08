package lila.round

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.*
import chess.Color
import chess.format.{ BoardFen, Fen }
import chess.{ Centis, Replay }
import play.api.libs.json.*
import scala.concurrent.ExecutionContext

import lila.common.Bus
import lila.common.Json.given
import lila.game.actorApi.{ FinishGame, MoveGameEvent }
import lila.game.{ Game, GameRepo }

final class ApiMoveStream(gameRepo: GameRepo, gameJsonView: lila.game.JsonView)(using
    ExecutionContext
):

  def apply(game: Game, delayMoves: Boolean): Source[JsObject, ?] =
    Source futureSource {
      val hasMoveDelay         = delayMoves && game.hasClock
      val delayMovesBy         = hasMoveDelay ?? 3
      val delayKeepsFirstMoves = hasMoveDelay ?? 5
      gameRepo.initialFen(game) map { initialFen =>
        val buffer = scala.collection.mutable.Queue.empty[JsObject]
        var moves  = 0
        Source(List(gameJsonView(game, initialFen))) concat
          Source
            .queue[JsObject]((game.ply.value + 3) atLeast 16, akka.stream.OverflowStrategy.dropHead)
            .statefulMapConcat { () => js =>
              moves += 1
              if (game.finished || moves <= delayKeepsFirstMoves) List(js)
              else
                buffer.enqueue(js)
                (buffer.size > delayMovesBy) ?? List(buffer.dequeue())
            }
            .mapMaterializedValue { queue =>
              val clocks = for {
                clk        <- game.clock
                clkHistory <- game.clockHistory
              } yield (
                Vector(clk.config.initTime) ++ clkHistory.white,
                Vector(clk.config.initTime) ++ clkHistory.black
              )
              val clockOffset = game.startColor.fold(0, 1)
              Replay.situations(game.sans, initialFen, game.variant) foreach {
                _.zipWithIndex foreach { case (s, index) =>
                  val clk = for {
                    (clkWhite, clkBlack) <- clocks
                    white                <- clkWhite.lift((index + 1 - clockOffset) >> 1)
                    black                <- clkBlack.lift((index + clockOffset) >> 1)
                  } yield (white, black)
                  queue offer toJson(
                    Fen writeBoard s.board,
                    s.color,
                    s.board.history.lastMove.map(_.uci),
                    clk
                  )
                }
              }
              if (game.finished)
                queue offer gameJsonView(game, initialFen)
                queue.complete()
              else
                val chans = List(MoveGameEvent makeChan game.id, "finishGame")
                val sub = Bus.subscribeFun(chans*) {
                  case MoveGameEvent(g, fen, move) =>
                    queue.offer(toJson(g, fen, move.some)).unit
                  case FinishGame(g, _, _) if g.id == game.id =>
                    queue offer gameJsonView(g, initialFen)
                    (1 to buffer.size) foreach { _ => queue.offer(Json.obj()) } // push buffer content out
                    queue.complete()
                }
                queue.watchCompletion() addEffectAnyway {
                  Bus.unsubscribe(sub, chans)
                }
            }
      }
    }

  private def toJson(game: Game, fen: BoardFen, lastMoveUci: Option[String]): JsObject =
    toJson(
      fen,
      game.turnColor,
      lastMoveUci,
      game.clock.map { clk =>
        (clk.remainingTime(chess.White), clk.remainingTime(chess.Black))
      }
    )

  private def toJson(
      fen: BoardFen,
      turnColor: Color,
      lastMoveUci: Option[String],
      clock: Option[(Centis, Centis)]
  ): JsObject =
    clock.foldLeft(
      Json
        .obj("fen" -> fen.andColor(turnColor))
        .add("lm" -> lastMoveUci)
    ) { case (js, clk) =>
      js ++ Json.obj("wc" -> clk._1.roundSeconds, "bc" -> clk._2.roundSeconds)
    }
