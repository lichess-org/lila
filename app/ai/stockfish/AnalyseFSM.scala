package lila
package ai.stockfish

import model._
import model.analyse._

import akka.actor.{ Props, Actor, ActorRef, FSM ⇒ AkkaFSM, LoggingFSM }
import scalaz.effects._

final class AnalyseFSM(
  processBuilder: (String ⇒ Unit, String ⇒ Unit) ⇒ Process,
  config: AnalyseConfig)
    extends Actor with LoggingFSM[State, Data] {

  val process = processBuilder(out ⇒ self ! Out(out), err ⇒ self ! Err(err))

  startWith(Starting, Todo())

  when(Starting) {
    case Event(Out(t), _) if t startsWith "Stockfish" ⇒ {
      process write "uci"
      stay
    }
    case Event(Out(t), data) if t contains "uciok" ⇒ {
      config.init foreach process.write
      nextAnalyse(data)
    }
    case Event(analyse: Analyse, data) ⇒
      stay using (data enqueue Task(analyse, sender))
  }
  when(Ready) {
    case Event(Out(t), _) ⇒ { log.warning(t); stay }
  }
  when(UciNewGame) {
    case Event(Out(t), data: Doing) if t contains "readyok" ⇒
      nextInfo(data)
  }
  when(Running) {
    case Event(Out(t), data: Doing) if t startsWith "info depth" ⇒ {
      goto(Running) using (data buffer t)
    }
    case Event(Out(t), data: Doing) if t contains "bestmove" ⇒
      (data buffer t).flush.fold(
        err ⇒ {
          log.error(err.shows)
          data.current.ref ! failure(err)
          nextAnalyse(data.done)
        },
        nextData ⇒ nextInfo(nextData)
      )
  }
  whenUnhandled {
    case Event(analyse: Analyse, data) ⇒
      nextAnalyse(data enqueue Task(analyse, sender))
    case Event(GetQueueSize, data)                       ⇒ 
      sender ! QueueSize(data.queue.size); stay
    case Event(Out(t), _) if isNoise(t) ⇒ stay
    case Event(Out(t), _)               ⇒ { log.warning(t); stay }
    case Event(Err(t), _)               ⇒ { log.error(t); stay }
  }

  def nextAnalyse(data: Data) = data match {
    case todo: Todo ⇒ todo.doing(
      doing ⇒ {
        config game doing.current.analyse foreach process.write
        process write "ucinewgame"
        process write "isready"
        goto(UciNewGame) using doing
      },
      t ⇒ goto(Ready) using t
    )
    case doing: Doing ⇒ stay using data
  }

  def nextInfo(doing: Doing) = doing.current |> { task ⇒
    (task.analyse go config.moveTime).fold(
      instructions ⇒ {
        instructions foreach process.write
        goto(Running) using doing
      }, {
        task.ref ! success(task.analyse.analysis.done)
        nextAnalyse(doing.done)
      })
  }

  def isNoise(t: String) =
    t.isEmpty || (t startsWith "id ") || (t startsWith "info ") || (t startsWith "option name ")

  def onTermination() {
    process.destroy()
  }
}
