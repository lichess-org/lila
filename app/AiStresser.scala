package lila.app

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import ornicar.scalalib.Random.approximatly

import makeTimeout.short

private[app] final class AiStresser(
    env: lila.ai.Env,
    system: ActorSystem,
    uciMemo: lila.game.UciMemo) {

  def apply {

    (1 to 80) foreach { i =>
      system.scheduler.scheduleOnce((i * 97) millis) {
        play(i % 8 + 1, true)
        // play(8, true)
      }
    }
    (1 to 5) foreach { i =>
      system.scheduler.scheduleOnce((i * 131) millis) {
        analyse(true)
      }
    }

  }

  private def play(level: Int, loop: Boolean) = system.actorOf(Props(new Actor {

    override def preStart {
      newGame pipeTo self
    }

    def receive = {
      case Game(moves, it) if it >= moves.size => if (loop) newGame pipeTo self
      case Game(moves, it) =>
        env.client.move(moves take it, none, level).effectFold(e => {
          logwarn("[ai] play: " + e)
          newGame pipeTo self
        }, { x =>
          system.scheduler.scheduleOnce(randomize(5 second)) {
            self ! Game(moves, it + 1)
          }
        })
    }
  }))

  private def analyse(loop: Boolean) = system.actorOf(Props(new Actor {

    override def preStart {
      newGame pipeTo self
    }

    def receive = {
      case Game(moves, _) =>
        env.client.analyse(moves, none).effectFold(e => {
          logwarn("[ai] server analyse: " + e)
          if (loop) newGame pipeTo self
        }, { _ =>
          loginfo("analyse complete")
          if (loop) newGame pipeTo self
        })
    }
  }))

  // private def newGame = fuccess {
  //   val pgn = "e3 Nc6 Nf3 Nf6 Nc3 d6 d3 Be6 d4 Rb8 b3 Rg8 g3 g5 Nxg5 Rxg5 f4 Bg4 fxg5 Bxd1 Kxd1 Ng4"
  //   // val pgn = "e3 Nc6 Nf3 Nf6"
  //   Game(pgn.split(' ').toList, 1)
  // }

  private def newGame = lila.game.GameRepo findRandomStandardCheckmate 30000 flatMap {
    case None    => fuccess(Game(Nil, 1))
    case Some(g) => uciMemo get g map { moves => Game(moves.toList, 1) }
  }

  private def randomize(d: FiniteDuration, ratio: Float = 0.1f): FiniteDuration =
    approximatly(ratio)(d.toMillis) millis

  private case class Game(moves: List[String], it: Int)
}
