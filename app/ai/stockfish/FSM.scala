package lila
package ai.stockfish

import model._

import akka.actor.{ Props, Actor, ActorRef, FSM ⇒ AkkaFSM, LoggingFSM }
import scalaz.effects._

final class FSM(
  processBuilder: (String ⇒ Unit, String ⇒ Unit) ⇒ Process)
    extends Actor with LoggingFSM[model.State, model.Data] {

  val process = processBuilder(out ⇒ self ! Out(out), err ⇒ self ! Err(err))

  startWith(Starting, Todo(Vector.empty))

  when(Starting) {
    case Event(Out(t), data) if t contains "Stockfish" ⇒ next(data)
    case Event(play @ Play(moves, _), data) ⇒
      stay using (data enqueue (play -> sender))
  }
  when(Ready) {
    case Event(Out(t), _) ⇒ { log.warning(t); stay }
  }
  when(UciNewGame) {
    case Event(Out(t), data @ Doing((play, _), _)) if t contains "readyok" ⇒ {
      process write "position %s moves %s".format(play.fen | "startpos", play.moves)
      process write "go movetime 9000"
      goto(Go)
    }
  }
  when(Go) {
    case Event(Out(t), data @ Doing((_, ref), _)) if t contains "bestmove" ⇒ {
      ref ! BestMove(t.split(' ') lift 1)
      goto(Ready) using data.done
    }
  }
  whenUnhandled {
    case Event(play @ Play(moves, _), data) ⇒ 
      next(data enqueue (play -> sender))
    case Event(Err(t), _) ⇒ { log.error(t); stay }
  }

  def next(data: Data) = data match {
    case todo: Todo ⇒ todo.doing(
      doing ⇒ {
        process write "ucinewgame"
        process write "isready"
        goto(UciNewGame) using doing
      },
      t ⇒ goto(Ready) using t
    )
    case doing: Doing ⇒ stay using data
  }

  def onTermination() {
    process.destroy()
  }
}
