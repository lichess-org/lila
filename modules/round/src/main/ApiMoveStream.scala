package lila.round

import akka.stream.scaladsl.*
import chess.format.Fen
import chess.{ Centis, Replay }
import play.api.libs.json.*

import lila.common.Bus
import lila.common.Json.given
import lila.game.actorApi.{ FinishGame, MoveGameEvent }
import lila.game.{ Game, GameRepo }

final class ApiMoveStream(
    gameRepo: GameRepo,
    gameJsonView: lila.game.JsonView,
    lightUserApi: lila.user.LightUserApi
)(using Executor):

  def apply(game: Game, delayMoves: Boolean): Source[JsObject, ?] =
    Source futureSource {
      val hasMoveDelay         = delayMoves && game.hasClock
      val delayMovesBy         = hasMoveDelay ?? 3
      val delayKeepsFirstMoves = hasMoveDelay ?? 5
      for
        initialFen <- gameRepo.initialFen(game)
        lightUsers <- lightUserApi.asyncManyOptions(game.players.map(_.userId))
      yield
        val buffer = scala.collection.mutable.Queue.empty[JsObject]
        var moves  = 0
        def makeGameJson(g: Game) =
          gameJsonView.base(g, initialFen) ++ Json.obj(
            "players" -> JsObject(g.players zip lightUsers map { (p, user) =>
              p.color.name -> gameJsonView.player(p, user)
            })
          )
        Source(List(makeGameJson(game))) concat
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
              val clocks = for
                clk        <- game.clock
                clkHistory <- game.clockHistory
              yield (
                Vector(clk.config.initTime) ++ clkHistory.white,
                Vector(clk.config.initTime) ++ clkHistory.black
              )
              val clockOffset = game.startColor.fold(0, 1)
              Replay.situations(game.sans, initialFen, game.variant) foreach {
                _.zipWithIndex foreach { (s, index) =>
                  val clk = for
                    (clkWhite, clkBlack) <- clocks
                    white                <- clkWhite.lift((index + 1 - clockOffset) >> 1)
                    black                <- clkBlack.lift((index + clockOffset) >> 1)
                  yield (white, black)
                  queue offer toJson(
                    Fen write s,
                    s.board.history.lastMove.map(_.uci),
                    clk
                  )
                }
              }
              if (game.finished)
                queue offer makeGameJson(game)
                queue.complete()
              else
                val chans = List(MoveGameEvent makeChan game.id, "finishGame")
                val sub = Bus.subscribeFun(chans*) {
                  case MoveGameEvent(g, fen, move) =>
                    queue.offer(toJson(g, fen, move.some)).unit
                  case FinishGame(g, _, _) if g.id == game.id =>
                    queue offer makeGameJson(g)
                    (1 to buffer.size) foreach { _ => queue.offer(Json.obj()) } // push buffer content out
                    queue.complete()
                }
                queue.watchCompletion() addEffectAnyway {
                  Bus.unsubscribe(sub, chans)
                }
            }
    }
  end apply

  private def toJson(game: Game, fen: Fen.Epd, lastMoveUci: Option[String]): JsObject =
    toJson(
      fen,
      lastMoveUci,
      game.clock.map { clk =>
        (clk.remainingTime(chess.White), clk.remainingTime(chess.Black))
      }
    )

  private def toJson(fen: Fen.Epd, lastMoveUci: Option[String], clock: Option[PairOf[Centis]]): JsObject =
    clock.foldLeft(
      Json
        .obj("fen" -> fen)
        .add("lm" -> lastMoveUci)
    ) { (js, clk) =>
      js ++ Json.obj("wc" -> clk._1.roundSeconds, "bc" -> clk._2.roundSeconds)
    }
