package lila.ai
package stockfish

import actorApi._
import akka.actor.{ Props, Actor, ActorRef, Status, FSM => AkkaFSM }

import lila.analyse.Analysis

private[stockfish] final class ActorFSM(
  processBuilder: Process.Builder,
  config: Config)
    extends Actor with AkkaFSM[State, Option[Job]] {

  private val process = processBuilder(
    out => self ! Out(out),
    err => self ! Err(err),
    config.debug)

  startWith(Starting, none)

  when(Starting) {
    case Event(Out(t), _) if t startsWith "Stockfish" => {
      process write "uci"
      stay
    }
    case Event(Out("uciok"), job) => {
      config.init foreach process.write
      loginfo("[ai] stockfish is ready")
      job.fold(goto(Idle))(start)
    }
    case Event(req: Req, none) => stay using Job(req, sender, Nil).some
  }
  when(Idle) {
    case Event(Out(t), _)   => { logwarn(t); stay }
    case Event(req: Req, _) => start(Job(req, sender, Nil))
  }
  when(IsReady) {
    case Event(Out("readyok"), Some(Job(req, _, _))) => {
      println(req match {
        case r: PlayReq => "P " + ("-" * (r.level))
        case r: AnalReq => "A " + ("=" * (Config.levelMax + 2))
      })
      config go req foreach process.write
      goto(Running)
    }
  }
  when(Running) {
    case Event(Out(t), Some(job)) if t startsWith "info depth" =>
      stay using (job + t).some
    case Event(Out(t), Some(job)) if t startsWith "bestmove" => {
      job.sender ! (job complete t)
      goto(Idle) using none
    }
  }
  whenUnhandled {
    case Event(req: Req, _) => {
      logerr("[stockfish] FSM unhandled request " + req)
      stay
    }
    case Event(Out(t), _) => stay
  }

  def start(job: Job) = job match {
    case Job(req, sender, _) => {
      config prepare req foreach process.write
      process write "isready"
      goto(IsReady) using Job(req, sender, Nil).some
    }
  }

  override def preStart() {
    loginfo("[stockfish] start FSM")
  }

  override def postStop() {
    loginfo("[stockfish] destroy FSM")
    process.destroy()
  }
}
