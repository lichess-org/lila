package lila.app

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import ornicar.scalalib.Random.approximatly

import makeTimeout.short

private[app] final class AiStresser(env: lila.ai.Env, system: ActorSystem) {

  def apply {

    (1 to 1024) foreach { i ⇒
      system.scheduler.scheduleOnce((i*97) millis) {
        play(i % 8 + 1)
      }
    }

  }

  private def play(level: Int) = system.actorOf(Props(new Actor {

    def newGame = lila.game.PgnRepo getOneRandom 30000 map { pgn ⇒
      Game((~pgn).split(' ').toList, 1)
    }

    override def preStart {
      newGame pipeTo self
    }

    def receive = {
      case Game(moves, it) if it >= moves.size ⇒ newGame pipeTo self
      case Game(moves, it) ⇒
        ai.play(moves take it mkString " ", none, level).effectFold(e ⇒ {
          logwarn("[ai] server play: " + e)
          newGame pipeTo self
        }, { _ ⇒
          system.scheduler.scheduleOnce(randomize(1 second)) {
            self ! Game(moves, it + 1)
          }
        })
    }
  }))

  private def randomize(d: FiniteDuration, ratio: Float = 0.1f): FiniteDuration =
    approximatly(ratio)(d.toMillis) millis

  private val ai = env.stockfishServer

  private case class Game(moves: List[String], it: Int)
}
