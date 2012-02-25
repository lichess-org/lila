package lila
package model

import Pos._
import scalaz.{ Success, Failure }
import format.Visual

case class Board(pieces: Map[Pos, Piece], history: History) {

  import implicitFailures._

  def apply(at: Pos): Option[Piece] = pieces get at

  def apply(x: Int, y: Int): Option[Piece] = pos(x, y) flatMap pieces.get

  def pieceAt(at: Pos): Valid[Piece] = apply(at) toSuccess ("No piece on " + at)

  lazy val actors: Map[Pos, Actor] = pieces map {
    case (pos, piece) ⇒ (pos, Actor(piece, pos, this))
  }

  lazy val colorActors: Map[Color, Iterable[Actor]] = actors.values groupBy (_.color)

  def actorsOf(c: Color) = colorActors get c getOrElse Nil

  def actorAt(at: Pos): Valid[Actor] = actors get at toSuccess ("No piece on " + at)

  def kingPosOf(c: Color): Option[Pos] = pieces find (_._2 == c.king) map (_._1)

  def movesFrom(from: Pos): Valid[Set[Pos]] = actorAt(from) map (_.moves)

  def seq(actions: Board ⇒ Valid[Board]*): Valid[Board] =
    actions.foldLeft(success(this): Valid[Board])(_ flatMap _)

  def place(piece: Piece) = new {
    def at(at: Pos): Valid[Board] =
      if (pieces contains at) failure("Cannot place at occupied " + at)
      else success(copy(pieces = pieces + ((at, piece))))
  }

  def takeValid(at: Pos): Valid[Board] = take(at) toSuccess ("No piece at " + at + " to take")

  def take(at: Pos): Option[Board] = pieces get at map { piece ⇒
    copy(pieces = (pieces - at))
  }

  def move(orig: Pos, dest: Pos): Option[Board] =
    if (pieces contains dest) None
    else pieces get orig map { piece ⇒
      copy(pieces = (pieces - orig) + ((dest, piece)))
    }

  def taking(orig: Pos, dest: Pos, taking: Option[Pos] = None): Option[Board] =
    take(taking getOrElse dest) flatMap { b ⇒ b.move(orig, dest) }

  def move(orig: Pos) = new {
    def to(dest: Pos): Valid[Board] = {
      if (pieces contains dest) failure("Cannot move to occupied " + dest)
      else pieces get orig map { piece ⇒
        copy(pieces = (pieces - orig) + ((dest, piece)))
      } toSuccess ("No piece at " + orig + " to move")
    }
  }

  lazy val occupation: Map[Color, Set[Pos]] = Color.all map { color ⇒
    (color, pieces collect { case (pos, piece) if piece is color ⇒ pos } toSet)
  } toMap

  lazy val occupations = pieces.keySet

  /**
   * Promote the piece at the given position to a new role
   * @return a new board
   */
  def promote(at: Pos) = new {
    def to(role: Role): Valid[Board] = role match {
      case King | Pawn ⇒ failure("Cannot promote to King or pawn")
      case _ ⇒ apply(at) match {
        case Some(piece) if piece.role == Pawn ⇒
          success(copy(pieces = pieces.updated(at, piece.color - role)))
        case _ ⇒ failure("No pawn at " + at + " to promote")
      }
    }
  }

  def withHistory(h: History): Board = copy(history = h)

  override def toString = Visual >> this
}

object Board {

  import Pos._

  def apply(pieces: Traversable[(Pos, Piece)]): Board = Board(pieces toMap, History())

  def apply(pieces: (Pos, Piece)*): Board = Board(pieces toMap, History())

  def apply(): Board = {

    val lineUp = IndexedSeq(Rook, Knight, Bishop, Queen, King, Bishop, Knight, Rook)

    val pairs = for (y ← Seq(1, 2, 7, 8); x ← 1 to 8) yield (Pos.unsafe(x, y), y match {
      case 1 ⇒ White - lineUp(x - 1)
      case 2 ⇒ White - Pawn
      case 7 ⇒ Black - Pawn
      case 8 ⇒ Black - lineUp(x - 1)
    })

    Board(pairs toMap, History())
  }

  def empty = new Board(Map.empty, History())
}
