package lila.chess

import Pos._
import scalaz.{ Success, Failure }
import format.Visual

case class Board(pieces: Map[Pos, Piece], history: History) {

  import implicitFailures._

  def apply(at: Pos): Option[Piece] = pieces get at

  def apply(x: Int, y: Int): Option[Piece] = makePos(x, y) flatMap pieces.get

  def pieceAt(at: Pos): Valid[Piece] = apply(at) toSuccess ("No piece on " + at)

  lazy val actors: Map[Pos, Actor] = pieces map {
    case (pos, piece) ⇒ (pos, Actor(piece, pos, this))
  }

  lazy val colorActors: Map[Color, List[Actor]] =
    actors.values groupBy (_.color) mapValues (_.toList)

  def actorsOf(c: Color): List[Actor] = colorActors get c getOrElse Nil

  def actorAt(at: Pos): Valid[Actor] = actors get at toSuccess ("No piece on " + at)

  lazy val kingPos: Map[Color, Pos] = pieces collect {
    case (pos, Piece(color, King)) ⇒ color -> pos
  } toMap

  def kingPosOf(c: Color): Option[Pos] = kingPos get c

  def movesFrom(from: Pos): Valid[Set[Pos]] = actorAt(from) map (_.moves)

  def seq(actions: Board ⇒ Valid[Board]*): Valid[Board] =
    actions.foldLeft(success(this): Valid[Board])(_ flatMap _)

  def place(piece: Piece) = new {
    def at(at: Pos): Valid[Board] =
      if (pieces contains at) failure("Cannot place at occupied " + at)
      else success(copy(pieces = pieces + ((at, piece))))
  }

  def place(piece: Piece, at: Pos) =
    if (pieces contains at) None
    else Some(copy(pieces = pieces + ((at, piece))))

  def takeValid(at: Pos): Valid[Board] = take(at) toSuccess ("No piece at " + at + " to take")

  def take(at: Pos): Option[Board] = pieces get at map { piece ⇒
    copy(pieces = pieces - at)
  }

  def move(orig: Pos, dest: Pos): Option[Board] =
    if (pieces contains dest) None
    else pieces get orig map { piece ⇒
      copy(pieces = pieces - orig + ((dest, piece)))
    }

  def taking(orig: Pos, dest: Pos, taking: Option[Pos] = None): Option[Board] = for {
    piece ← pieces get orig
    takenPos = taking getOrElse dest
    if (pieces contains takenPos)
  } yield copy(pieces = pieces - takenPos - orig + ((dest, piece)))

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

  def promote(orig: Pos, dest: Pos): Option[Board] = for {
    pawn ← apply(orig)
    b1 ← move(orig, dest)
    b2 ← b1.take(dest)
    b3 ← b2.place(pawn.color.queen, dest)
  } yield b3

  def withHistory(h: History): Board = copy(history = h)

  def updateHistory(f: History ⇒ History) = copy(history = f(history))

  def as(c: Color) = Situation(this, c)

  def count(p: Piece) = pieces.values count (_ == p)

  def visual = Visual >> this

  override def toString = visual
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
