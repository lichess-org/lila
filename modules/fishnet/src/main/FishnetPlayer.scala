package lila.fishnet

import chess.{ Black, Clock, White }
import scalalib.ThreadLocalRandom

import lila.common.{ Bus, LilaFuture }
import lila.core.round.{ Tell, RoundBus }

final class FishnetPlayer(
    redis: FishnetRedis,
    openingBook: FishnetOpeningBook,
    gameRepo: lila.core.game.GameRepo,
    uciMemo: lila.core.game.UciMemo
)(using Executor, Scheduler):

  def apply(game: Game): Funit =
    game.aiLevel
      .so: level =>
        LilaFuture.delay(delayFor(game) | 0.millis):
          openingBook(game, level).flatMap:
            case Some(move) =>
              uciMemo
                .sign(game)
                .map: sign =>
                  Bus.pub(Tell(game.id, RoundBus.FishnetPlay(move, sign)))
            case None => makeWork(game, level).addEffect(redis.request).void
      .recover { case e: Exception =>
        logger.info(e.getMessage)
      }

  private val delayFactor = 0.011f
  private val defaultClock = Clock(Clock.LimitSeconds(300), Clock.IncrementSeconds(0))

  private def delayFor(g: Game): Option[FiniteDuration] =
    if !g.bothPlayersHaveMoved then 2.seconds.some
    else
      for
        pov <- g.aiPov
        clock = g.clock | defaultClock
        totalTime = clock.estimateTotalTime.centis
        if totalTime > 20 * 100
        delay = (clock.remainingTime(pov.color).centis.atMost(totalTime)) * delayFactor
        accel = 1 - ((g.ply.value - 20).atLeast(0).atMost(100)) / 150f
        sleep = (delay * accel).atMost(500)
        if sleep > 25
        millis = sleep * 10
        randomized = millis + millis * (ThreadLocalRandom.nextDouble() - 0.5)
        divided = randomized / (if g.ply > 9 then 1 else 2)
      yield divided.toInt.millis

  private def makeWork(game: Game, level: Int): Fu[Work.Move] =
    if game.position.playable(true) then
      if game.ply <= lila.core.fishnet.maxPlies then
        gameRepo.initialFen(game).zip(uciMemo.get(game)).map { case (initialFen, moves) =>
          Work.Move(
            _id = Work.makeId,
            game = Work.Game(
              id = game.id.value,
              initialFen = initialFen,
              studyId = none,
              variant = game.variant,
              moves = moves.mkString(" ")
            ),
            level =
              if level < 3 && game.clock.exists(_.config.limitSeconds < 60) then 3
              else level,
            clock = game.clock.map { clk =>
              Work.Clock(
                wtime = clk.remainingTime(White).centis,
                btime = clk.remainingTime(Black).centis,
                inc = clk.incrementSeconds
              )
            }
          )
        }
      else fufail(s"[fishnet] Too many moves (${game.ply}), won't play ${game.id}")
    else fufail(s"[fishnet] invalid position on ${game.id}")
