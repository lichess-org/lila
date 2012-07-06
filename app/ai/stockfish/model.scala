package lila
package ai.stockfish

import chess.Pos.posAt
import chess.format.UciMove
import analyse.{ Analysis, AnalysisBuilder }

import akka.actor.ActorRef
import collection.immutable.Queue

object model {

  object play {

    case class Play(moves: String, fen: Option[String], level: Int) {
      def go(moveTime: Int ⇒ Int) = List(
        "position %s moves %s".format(fen.fold("fen " + _, "startpos"), moves),
        "go movetime %d depth %d".format(moveTime(level), level)
      )
      def chess960 = fen.isDefined
    }
    case class BestMove(move: Option[String]) {
      def parse = UciMove(move | "")
    }

    case class Task(play: Play, ref: ActorRef)

    sealed trait Data {
      def queue: Queue[Task]
      def enqueue(task: Task): Data
    }
    case class Todo(queue: Queue[Task] = Queue.empty) extends Data {
      def enqueue(task: Task) = copy(queue = queue :+ task)
      def doing[A](withTask: Doing ⇒ A, without: Todo ⇒ A) =
        queue.headOption.fold(
          task ⇒ withTask(Doing(task, queue.tail)),
          without(Todo(Queue.empty))
        )
    }
    case class Doing(current: Task, queue: Queue[Task]) extends Data {
      def enqueue(task: Task) = copy(queue = queue :+ task)
      def done = Todo(queue)
    }
  }

  object analyse {

    case class Analyse(
        moves: IndexedSeq[String],
        fen: Option[String],
        analysis: AnalysisBuilder,
        infoBuffer: List[String]) {

      def go(moveTime: Int) = nextMove.isDefined option List(
        "position %s moves %s".format(
          fen.fold("fen " + _, "startpos"),
          moves take analysis.size mkString " "),
        "go movetime %d".format(moveTime)
      )

      def nextMove = moves lift analysis.size

      def buffer(str: String) = copy(infoBuffer = str :: infoBuffer)

      def flush = for {
        move ← nextMove toValid "No move to flush"
        info ← AnalyseParser(infoBuffer)(move)
      } yield copy(
        analysis = analysis + info,
        infoBuffer = Nil)

      def chess960 = fen.isDefined
    }
    object Analyse {
      def apply(moves: String, fen: Option[String]) = new Analyse(
        moves = moves.split(' ').toIndexedSeq,
        fen = fen,
        analysis = Analysis.builder,
        infoBuffer = Nil)
    }

    case class Task(analyse: Analyse, ref: ActorRef) {
      def buffer(str: String) = copy(analyse = analyse buffer str)
      def flush = analyse.flush map { a ⇒ copy(analyse = a) }
    }

    sealed trait Data {
      def queue: Queue[Task]
      def enqueue(task: Task): Data
    }
    case class Todo(queue: Queue[Task] = Queue.empty) extends Data {
      def enqueue(task: Task) = copy(queue = queue :+ task)
      def doing[A](withTask: Doing ⇒ A, without: Todo ⇒ A) =
        queue.headOption.fold(
          task ⇒ withTask(Doing(task, queue.tail)),
          without(Todo(Queue.empty))
        )
    }
    case class Doing(current: Task, queue: Queue[Task]) extends Data {
      def enqueue(task: Task) = copy(queue = queue :+ task)
      def done = Todo(queue)
      def buffer(str: String) = copy(current = current buffer str)
      def flush = current.flush map { c ⇒ copy(current = c) }
    }
  }

  sealed trait State
  case object Starting extends State
  case object Ready extends State
  case object UciNewGame extends State
  case object Running extends State

  sealed trait Stream { def text: String }
  case class Out(text: String) extends Stream
  case class Err(text: String) extends Stream

  case object GetQueueSize
  case class QueueSize(i: Int)
}
