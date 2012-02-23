package lila
package model

import Pos._

case class Board(pieces: Map[Pos, Piece]) {

  def apply(at: Pos): Option[Piece] = pieces get at

  def apply(x: Int, y: Int): Option[Piece] = pos(x, y) flatMap pieces.get

  def place(piece: Piece) = new {
    def at(at: Pos): Valid[Board] =
      if (pieces contains at) failure("Cannot move to occupied " + at)
      else success(copy(pieces = pieces + ((at, piece))))
  }

  def take(at: Pos): Valid[Board] = pieces get at map { piece ⇒
    copy(pieces = (pieces - at))
  } toSuccess ("No piece at " + at + " to take")

  def move(orig: Pos) = new {
    def to(dest: Pos): Valid[Board] = {
      if (pieces contains dest) failure("Cannot move to occupied " + dest)
      else pieces get orig map { piece ⇒
        copy(pieces = (pieces - orig) + ((dest, piece)))
      } toSuccess ("No piece at " + orig + " to move")
    }
  }

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

  /**
   * Finds all positions which contain a threat to the given color (at a given position).
   * Note, will not identify threats to position from en passant (as this behaviour is not required).
   * @return the positions of threat
   */
  //def threatsTo(color: Color) = {
  //case class Threat() {
  //def at(s: Symbol): List[Pos] = at(position(s))

  //def at(pos: Pos): List[Pos] = {
  //def expand(direction: Seq[Option[Pos] => Option[Pos]]): Option[Pos] = {
  //def next(from: Option[Pos]): Option[Pos] = {
  //val nextPose = direction.foldLeft(from) {(acc, next) => next(acc)}
  //if (nextPose.isEmpty) return None
  //if (pieces.contains(nextPose.get)) Some(nextPose.get) else next(nextPose)
  //}
  //next(Some(pos))
  //}

  //def opposing(r: Role) = {
  //case class OpposingRoleCheck() {
  //def at(p: Pos) = pieces.get(p).map(_.equals(Piece(opposite of color, r))).getOrElse(false)
  //}
  //new OpposingRoleCheck
  //}

  //val forward = if (White.equals(color)) ^ _ else v _
  //val pawns: List[Pos] = Set(forward(pos < 1), forward(pos > 1)).filter(_.isDefined).foldLeft(Nil: List[Pos]) {
  //(acc, next) =>
  //if (opposing(Pawn).at(next.get)) next.get :: acc else acc
  //}
  //val rankFileVectors: List[Pos] = (expand(Seq(< _)) :: expand(Seq(^ _)) :: expand(Seq(> _)) :: expand(Seq(v _)) :: Nil)
  //.filter(_.isDefined).foldLeft(Nil: List[Pos]) {
  //(acc, next) =>
  //if (opposing(Rook).at(next.get) || opposing(Queen).at(next.get)) next.get :: acc else acc
  //}
  //val diagonalVectors: List[Pos] = (expand(Seq(< _, ^ _)) :: expand(Seq(> _, ^ _)) :: expand(Seq(> _, v _)) :: expand(Seq(< _, v _)) :: Nil)
  //.filter(_.isDefined).foldLeft(Nil: List[Pos]) {
  //(acc, next) =>
  //if (opposing(Bishop).at(next.get) || opposing(Queen).at(next.get)) next.get :: acc else acc
  //}
  //val knights: List[Pos] = {
  //val options = radialBasedPoss(pos, List(-2, -1, 1, 2), (rank, file) => Math.abs(rank) != Math.abs(file))
  //options.toList.filter(opposing(Knight).at(_))
  //}
  //val kings: List[Pos] = {
  //val options = radialBasedPoss(pos, -1 to 1, (rank, file) => (rank != 0 || file != 0))
  //options.toList.filter(opposing(King).at(_))
  //}

  //pawns ::: rankFileVectors ::: diagonalVectors ::: knights ::: kings
  //}
  //}
  //new Threat
  //}

  /**
   * Layout the board for a new game.
   * @return a new board
   */
}

object Board {

  import Pos._

  def apply(pieces: Traversable[(Pos, Piece)]): Board = Board(pieces toMap)

  def apply(): Board = {

    val lineUp = Seq(Rook, Knight, Bishop, Queen, King, Bishop, Knight, Rook)

    val pairs = for (y ← Seq(1, 2, 7, 8); x ← 1 to 8) yield (Pos.unsafe(x, y), y match {
      case 1 ⇒ Piece(White, lineUp(x - 1))
      case 2 ⇒ Piece(White, Pawn)
      case 7 ⇒ Piece(Black, Pawn)
      case 8 ⇒ Piece(Black, lineUp(x - 1))
    })

    new Board(pairs toMap)
  }

  def empty = Board(Map.empty)
}
