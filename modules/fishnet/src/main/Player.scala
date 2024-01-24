package lila.fishnet

import scala.concurrent.duration._
import org.joda.time.DateTime

import shogi.{ Clock, Gote, Sente }

import lila.common.Future
import lila.game.{ Game, GameRepo }
import ornicar.scalalib.Random.approximately

final class Player(
    moveDb: MoveDB,
    gameRepo: GameRepo,
    val maxPlies: Int
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  def apply(game: Game): Funit =
    game.aiEngine ?? { engine =>
      Future.delay(delayFor(game) | 0.millis) {
        makeWork(game, engine) addEffect moveDb.add void
      }
    } recover { case e: Exception =>
      logger.info(e.getMessage)
    }

  private val delayFactor  = 0.011f
  private val defaultClock = Clock(300, 0, 0, 0)

  private def delayFor(g: Game): Option[FiniteDuration] =
    if (!g.bothPlayersHaveMoved) 2.seconds.some
    else
      for {
        pov <- g.aiPov
        clock     = g.clock | defaultClock
        totalTime = clock.estimateTotalTime.centis
        if totalTime > 20 * 100
        delay = (clock.currentClockFor(pov.color).time.centis atMost totalTime) * delayFactor
        accel = 1 - ((g.plies - 20) atLeast 0 atMost 100) / 150f
        sleep = (delay * accel) atMost 500
        if sleep > 25
        millis     = sleep * 10
        randomized = approximately(0.5f)(millis)
        divided    = randomized / (if (g.plies > 9) 1 else 2)
      } yield divided.millis

  private def makeWork(game: Game, ec: lila.game.EngineConfig): Fu[Work.Move] =
    if (game.situation.playable(true, true))
      if (game.plies <= maxPlies)
        fuccess(
          Work.Move(
            _id = Work.makeId,
            game = Work.Game(
              id = game.id,
              initialSfen = game.initialSfen,
              studyId = none,
              variant = game.variant,
              moves = game.usiMoves.map(_.usi) mkString " "
            ),
            level = ec.level,
            engine = ec.engine.name,
            clock = game.clock.map { clk =>
              Work.Clock(
                btime = clk.currentClockFor(Sente).time.centis,
                wtime = clk.currentClockFor(Gote).time.centis,
                inc = clk.incrementSeconds,
                byo = clk.byoyomiSeconds
              )
            },
            tries = 0,
            lastTryByKey = none,
            acquired = none,
            createdAt = DateTime.now
          )
        )
      else fufail(s"[fishnet] Too many moves (${game.plies}), won't play ${game.id}")
    else fufail(s"[fishnet] invalid position on ${game.id}")
}
