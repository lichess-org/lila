package lila
package ai.stockfish

import model._
import model.play._

import akka.actor.{ Props, Actor, ActorRef, FSM ⇒ AkkaFSM }
import scalaz.effects._

final class PlayFSM(
  processBuilder: (String ⇒ Unit, String ⇒ Unit) ⇒ Process,
  config: PlayConfig)
    extends Actor with AkkaFSM[State, Data] {

  val process = processBuilder(out ⇒ self ! Out(out), err ⇒ self ! Err(err))

  startWith(Starting, Todo())

  when(Starting) {
    case Event(Out(t), _) if t startsWith "Stockfish" ⇒ {
      process write "uci"
      stay
    }
    case Event(Out(t), data) if t contains "uciok" ⇒ {
      config.init foreach process.write
      next(data)
    }
    case Event(play: Play, data) ⇒
      stay using (data enqueue Task(play, sender))
  }
  when(Ready) {
    case Event(Out(t), _) ⇒ { log.warning(t); stay }
  }
  when(UciNewGame) {
    case Event(Out(t), data @ Doing(Task(play, _), _)) if t contains "readyok" ⇒ {
      play.go foreach process.write
      goto(Running)
    }
  }
  when(Running) {
    case Event(Out(t), data @ Doing(Task(_, ref), _)) if t contains "bestmove" ⇒ {
      ref ! BestMove(t.split(' ') lift 1)
      next(data.done)
    }
  }
  whenUnhandled {
    case Event(play: Play, data) ⇒
      next(data enqueue Task(play, sender))
    case Event(GetQueueSize, data) ⇒
      sender ! QueueSize(data.queue.size); stay
    case Event(Out(t), _) if isNoise(t) ⇒ stay
    case Event(Err(t), _)               ⇒ { log.error(t); stay }
  }

  def next(data: Data) = data match {
    case todo: Todo ⇒ todo.doing(
      doing ⇒ {
        config game doing.current.play foreach process.write
        process write "ucinewgame"
        process write "isready"
        goto(UciNewGame) using doing
      },
      t ⇒ goto(Ready) using t
    )
    case doing: Doing ⇒ stay using data
  }

  def isNoise(t: String) =
    t.isEmpty || (t startsWith "id ") || (t startsWith "info ") || (t startsWith "option name ")

  def onTermination() {
    process.destroy()
  }
}
