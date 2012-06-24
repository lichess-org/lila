package lila
package ai.stockfish

import chess.Pos.posAt
import analyse.{ Analysis, AnalysisBuilder }

import akka.actor.ActorRef

object model {

  object play {

    case class Play(moves: String, fen: Option[String], level: Int) {
      def go(moveTime: Int ⇒ Int) = List(
        "position %s moves %s".format(fen.fold("fen " + _, "startpos"), moves),
        "go movetime %d" format moveTime(level)
      )
      def chess960 = fen.isDefined
    }
    case class BestMove(move: Option[String]) {
      def parse = Uci parseMove (move | "")
    }

    case class Task(play: Play, ref: ActorRef)

    sealed trait Data {
      def queue: Vector[Task]
      def enqueue(task: Task): Data
    }
    case class Todo(queue: Vector[Task]) extends Data {
      def enqueue(task: Task) = copy(queue = queue :+ task)
      def doing[A](withTask: Doing ⇒ A, without: Todo ⇒ A) =
        easierTaskInQueue.fold(
          task ⇒ withTask(Doing(task, queue.tail)),
          without(Todo(Vector.empty))
        )
      private def easierTaskInQueue = queue sortBy (_.play.level) headOption
    }
    case class Doing(current: Task, queue: Vector[Task]) extends Data {
      def enqueue(task: Task) = copy(queue = queue :+ task)
      def done = Todo(queue)
    }
  }

  object analyse {

    case class Analyse(
        moves: List[String],
        fen: Option[String],
        analysis: AnalysisBuilder,
        infoBuffer: List[String]) {

      def go(moveTime: Int) = moves lift (analysis.size + 1) map { nextMove ⇒
        List(
          "position %s moves %s".format(
            fen.fold("fen " + _, "startpos"),
            moves take analysis.size mkString " "),
          "go movetime %d searchmoves %s".format(moveTime, nextMove)
        )
      }

      def buffer(str: String) = copy(infoBuffer = str :: infoBuffer)

      def flush = copy(
        analysis = analysis + AnalyseParser(infoBuffer),
        infoBuffer = Nil)

      def chess960 = fen.isDefined
    }
    object Analyse {
      def apply(moves: String, fen: Option[String]) = new Analyse(
        moves = moves.split(' ').toList,
        fen = fen,
        analysis = Analysis.builder,
        infoBuffer = Nil)
    }

    case class Task(analyse: Analyse, ref: ActorRef) {
      def buffer(str: String) = copy(analyse = analyse buffer str)
      def flush = copy(analyse = analyse.flush)
    }

    sealed trait Data {
      def queue: Vector[Task]
      def enqueue(task: Task): Data
    }
    case class Todo(queue: Vector[Task]) extends Data {
      def enqueue(task: Task) = copy(queue = queue :+ task)
      def doing[A](withTask: Doing ⇒ A, without: Todo ⇒ A) =
        queue.headOption.fold(
          task ⇒ withTask(Doing(task, queue.tail)),
          without(Todo(Vector.empty))
        )
    }
    case class Doing(current: Task, queue: Vector[Task]) extends Data {
      def enqueue(task: Task) = copy(queue = queue :+ task)
      def done = Todo(queue)
      def buffer(str: String) = copy(current = current buffer str)
      def flush = copy(current = current.flush)
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
}
