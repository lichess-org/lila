package lila.ai
package stockfish

import chess.Pos.posAt
import chess.format.UciMove
// import analyse.{ Analysis, AnalysisBuilder }

import akka.actor.ActorRef

object model {

  type Task = Either[play.Task, analyse.Task]

  sealed trait Data {
    def queue: Vector[Task]
    def enqueue(task: Task): Data
    def enqueue(task: play.Task): Data = enqueue(Left(task))
    def enqueue(task: analyse.Task): Data = enqueue(Right(task))
    def size = queue.size
    def dequeue: Option[(Task, Vector[Task])] =
      queue find (_.isLeft) orElse queue.headOption map { task ⇒
        task -> queue.filter(task !=)
      }
    def fold[A](todo: Todo ⇒ A, doing: Doing ⇒ A): A

    override def toString = getClass.getName + " = " + queue.size
  }
  case class Todo(queue: Vector[Task] = Vector.empty) extends Data {
    def doing[A](withTask: Doing ⇒ A, without: Todo ⇒ A) = dequeue.fold(without(this)) {
      case (task, rest) ⇒ withTask(Doing(task, rest))
    }
    def fold[A](todo: Todo ⇒ A, doing: Doing ⇒ A): A = todo(this)
    def enqueue(task: Task) = copy(queue :+ task)
  }
  case class Doing(current: Task, queue: Vector[Task]) extends Data {
    def done = Todo(queue)
    def fold[A](todo: Todo ⇒ A, doing: Doing ⇒ A): A = doing(this)
    def enqueue(task: Task) = copy(queue = queue :+ task)
    def map(f: Task ⇒ Task): Doing = copy(current = f(current))
    def name = current.fold(_ ⇒ "SFP", _ ⇒ "SFA")
  }

  object play {

    case class Task(
        moves: String,
        fen: Option[String],
        level: Int,
        ref: ActorRef) {
      def chess960 = fen.isDefined
    }

    object Task {
      case class Builder(moves: String, fen: Option[String], level: Int) {
        def apply(sender: ActorRef) = new Task(moves, fen, level, sender)
      }
    }

    case class BestMove(move: Option[String]) {
      def parse = UciMove(move | "")
    }
  }

  object analyse {

    case class Task(
        moves: IndexedSeq[String],
        fen: Option[String],
        analysis: AnalysisBuilder,
        infoBuffer: List[String],
        ref: ActorRef) {
      def pastMoves: String = moves take analysis.size mkString " "
      def nextMove: Option[String] = moves lift analysis.size
      def isDone = nextMove.isEmpty
      def buffer(str: String) = copy(infoBuffer = str :: infoBuffer)
      def flush = for {
        move ← nextMove toValid "No move to flush"
        info ← AnalyseParser(infoBuffer)(move)
      } yield copy(analysis = analysis + info, infoBuffer = Nil)
      def chess960 = fen.isDefined
    }

    object Task {
      case class Builder(moves: String, fen: Option[String]) {
        def apply(sender: ActorRef) = new Task(
          moves = moves.split(' ').toIndexedSeq,
          fen = fen,
          analysis = Analysis.builder,
          infoBuffer = Nil,
          sender)
      }
    }
  }

  sealed trait State
  case object Starting extends State
  case object Idle extends State
  case object IsReady extends State
  case object Running extends State

  sealed trait Stream { def text: String }
  case class Out(text: String) extends Stream
  case class Err(text: String) extends Stream

  case object GetQueueSize
  case class QueueSize(i: Int)

  case object RebootException extends RuntimeException("The actor timed out")
}
