package lila
package ai.stockfish

import chess.Pos.posAt

import akka.actor.ActorRef

object model {

  case class Play(moves: String, fen: Option[String], level: Int) {
    def position = "position %s moves %s".format(
      fen.fold("fen " + _, "startpos"),
      moves)
    def go(moveTime: Int ⇒ Int) = "go movetime %d" format moveTime(level)
    def chess960 = fen.isDefined
  }
  case class BestMove(move: Option[String]) {
    def parse = for {
      m ← move
      orig ← posAt(m take 2)
      dest ← posAt(m drop 2)
    } yield orig -> dest
  }

  sealed trait State
  case object Starting extends State
  case object Ready extends State
  case object UciNewGame extends State
  case object Go extends State

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

  sealed trait Stream { def text: String }
  case class Out(text: String) extends Stream
  case class Err(text: String) extends Stream
}
