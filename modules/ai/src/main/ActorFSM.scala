package lila.ai

import actorApi._
import akka.actor.{ Props, Actor, ActorRef, Status, FSM => AkkaFSM }

import lila.analyse.Analysis

private[ai] final class ActorFSM(
    name: String,
    processBuilder: Process.Builder,
    config: Config,
    logger: String => Unit) extends Actor with AkkaFSM[State, Option[Job]] {

  private var lastWrite = List[String]()

  private val process = processBuilder(
    out => self ! Out(out),
    err => self ! Err(err),
    config.debug)

  startWith(Starting, none)

  when(Starting) {
    case Event(Out(t), _) if t startsWith "PolyGlot" =>
      process write "uci"
      stay
    case Event(Out("uciok"), job) =>
      config.init foreach process.write
      loginfo(s"[$name] stockfish polyglot is ready")
      job.fold(goto(Idle))(start)
    case Event(req: Req, none) => stay using Job(req, sender, None).some
  }
  when(Idle) {
    case Event(Out(t), _) if t.nonEmpty =>
      logwarn(s"""[$name] Unexpected engine output: "$t"""")
      stay
    case Event(req: Req, _) => start(Job(req, sender, None))
  }
  when(IsReady) {
    case Event(Out("readyok"), Some(Job(req, _, _))) =>
      logger(req match {
        case r: PlayReq => s"$name P ${"-" * (r.level)}"
        case r: AnalReq => s"$name A ${"#" * (Config.levelMax + 2)}"
      })
      lastWrite = config go req
      lastWrite foreach process.write
      goto(Running)
  }
  when(Running) {
    case Event(Out(t), Some(job)) if (t startsWith "info depth") && relevantLine(t) =>
      stay using (job + t).some
    case Event(Out(t), Some(job)) if t startsWith "bestmove" =>
      job.sender ! (job complete t)
      goto(Idle) using none
  }
  whenUnhandled {
    case Event(req: Req, _) => sys error s"[$name] FSM unhandled request $req"
    case Event(Out(t), _)   => stay
  }

  def start(job: Job) = job match {
    case Job(req, sender, _) =>
      config prepare req foreach process.write
      process write "isready"
      goto(IsReady) using Job(req, sender, None).some
  }

  private def relevantLine(l: String) =
    !(l contains "currmovenumber") &&
      !(l contains "lowerbound") &&
      !(l contains "upperbound")

  override def postStop() {
    println(s"======== $name\n${lastWrite mkString "\n"}\n========")
    process.destroy()
  }
}
