package lila.fishnet

import org.joda.time.DateTime
import scala.concurrent.duration._

import chess.format.{ FEN, Forsyth }
import chess.{ White, Black, Clock }

import lila.common.Future
import lila.game.{ Game, GameRepo, UciMemo }
import ornicar.scalalib.Random.approximatly

final class Player(
    moveDb: MoveDB,
    uciMemo: UciMemo,
    val maxPlies: Int
)(implicit system: akka.actor.ActorSystem) {

  def apply(game: Game): Funit = game.aiLevel ?? { level =>
    Future.delay(delayFor(game) | 0.millis) {
      makeWork(game, level) addEffect moveDb.add void
    }
  } recover {
    case e: Exception => logger.info(e.getMessage)
  }

  private val delayFactor = 0.011f
  private val defaultClock = Clock(300, 0)

  private def delayFor(g: Game): Option[FiniteDuration] =
    if (!g.bothPlayersHaveMoved) 4.seconds.some
    else for {
      pov <- g.aiPov
      clock = g.clock | defaultClock
      totalTime = clock.estimateTotalTime.centis
      if totalTime > 20 * 100
      delay = (clock.remainingTime(pov.color).centis atMost totalTime) * delayFactor
      accel = 1 - ((g.turns - 20) atLeast 0 atMost 100) / 150f
      sleep = (delay * accel) atMost 500
      if sleep > 25
      millis = sleep * 10
      randomized = approximatly(0.5f)(millis)
      divided = randomized / (if (g.turns > 9) 1 else 2)
    } yield divided.millis

  private def makeWork(game: Game, level: Int): Fu[Work.Move] =
    if (game.situation playable true)
      if (game.turns <= maxPlies) GameRepo.initialFen(game) zip uciMemo.get(game) map {
        case (initialFen, moves) => Work.Move(
          _id = Work.makeId,
          game = Work.Game(
            id = game.id,
            initialFen = initialFen,
            studyId = none,
            variant = game.variant,
            moves = moves mkString " "
          ),
          currentFen = FEN(Forsyth >> game.chess),
          level =
            if (level < 3 && game.clock.exists(_.config.limit.toSeconds < 60)) 3
            else level,
          clock = game.clock.map { clk =>
            Work.Clock(
              wtime = clk.remainingTime(White).centis,
              btime = clk.remainingTime(Black).centis,
              inc = clk.incrementSeconds
            )
          },
          tries = 0,
          lastTryByKey = none,
          acquired = none,
          createdAt = DateTime.now
        )
      }
      else fufail(s"[fishnet] Too many moves (${game.turns}), won't play ${game.id}")
    else fufail(s"[fishnet] invalid position on ${game.id}")
}
