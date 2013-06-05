package lila.ai
package stockfish

import akka.actor.{ Props, Actor, ActorRef, Status, FSM ⇒ AkkaFSM }
import model._
import model.analyse._

import actorApi._

final class ActorFSM(
  processBuilder: Process.Builder,
  config: Config)
    extends Actor with AkkaFSM[State, Option[(Req, ActorRef)]] {

  private val process = processBuilder(
    out ⇒ self ! Out(out),
    err ⇒ self ! Err(err),
    config.debug)

  startWith(Starting, none)

  when(Starting) {
    case Event(Out(t), data) if t startsWith "Stockfish" ⇒ {
      process write "uci"
      stay
    }
    case Event(Out("uciok"), data) ⇒ {
      config.init foreach process.write
      data.fold(goto(Idle))(start)
    }
    case Event(req: Req, none) ⇒ stay using (req, sender).some
  }
  when(Idle) {
    case Event(Out(t), _)   ⇒ { logwarn(t); stay }
    case Event(req: Req, _) ⇒ start(req, sender)
  }
  when(IsReady) {
    case Event(Out("readyok"), Some((req, _))) ⇒ {
      val lines = config go req
      lines.lastOption foreach { line ⇒
        println(req.analyse.fold("A", "P") + line.replace("go movetime", ""))
      }
      lines foreach process.write
      goto(Running)
    }
  }
  when(Running) {
    // TODO accumulate output for analysis parsing
    // case Event(Out(t), Some(req)) if t startsWith "info depth" ⇒
    //   stay using (doing map (_.right map (_ buffer t)))
    case Event(Out(t), Some((req, sender))) if t startsWith "bestmove" ⇒ {
      sender ! req.analyse.fold(
        Status.Failure(new Exception("Not implemented")),
        BestMove(t.split(' ') lift 1))
      goto(Idle) using none
    }
  }
  whenUnhandled {
    case Event(req: Req, _) ⇒ {
      logerr("[ai] stockfish FSM unhandled request " + req)
      stay
    }
    case Event(Out(t), _) ⇒ stay
  }

  def start(data: (Req, ActorRef)) = data match {
    case (req, sender) ⇒ {
      config prepare req foreach process.write
      process write "isready"
      goto(IsReady) using (req, sender).some
    }
  }

  override def postStop() {
    process.destroy()
  }
}
