package shogi

import shogi.variant.Variant

case class Board(pieces: PieceMap) {

  def apply(at: Pos): Option[Piece] = pieces get at

  def apply(x: Int, y: Int): Option[Piece] = Pos.at(x, y) flatMap pieces.get

  def piecesOf(c: Color): List[Piece] =
    pieces.values
      .collect {
        case piece if piece is c => piece
      }
      .toList

  lazy val kingPos: Map[Color, Pos] = pieces.collect { case (pos, Piece(color, King)) =>
    color -> pos
  }

  def kingPosOf(c: Color): Option[Pos] = kingPos get c

  def seq(actions: Board => Option[Board]*): Option[Board] =
    actions.foldLeft[Option[Board]](Some(this))(_ flatMap _)

  def place(piece: Piece, at: Pos): Option[Board] =
    if (pieces contains at) None
    else Some(copy(pieces = pieces + (at -> piece)))

  def forcePlace(piece: Piece, at: Pos): Board =
    copy(pieces = pieces + (at -> piece))

  def take(at: Pos): Option[Board] =
    if (pieces contains at) Some(copy(pieces = pieces - at))
    else None

  def forceTake(at: Pos): Board =
    copy(pieces = pieces - at)

  def move(orig: Pos, dest: Pos): Option[Board] =
    pieces get orig map { piece =>
      copy(pieces = pieces - orig + (dest -> piece))
    }

  def forceMove(piece: Piece, orig: Pos, dest: Pos): Board =
    copy(pieces = pieces - orig + (dest -> piece))

  def hasPiece(p: Piece) = pieces.values exists (p ==)

  def promote(orig: Pos, dest: Pos, promotion: Role => Option[Role]): Option[Board] =
    for {
      piece         <- apply(orig)
      promotedPiece <- piece.updateRole(promotion)
    } yield copy(pieces = pieces - orig + (dest -> promotedPiece))

  def withPieces(newPieces: PieceMap) = copy(pieces = newPieces)

  def count(p: Piece): Int = pieces.values count (_ == p)
  def count(r: Role): Int  = pieces.values count (_.role == r)
  def count(c: Color): Int = pieces.values count (_.color == c)

}

object Board {

  def apply(pieces: Iterable[(Pos, Piece)]): Board =
    new Board(pieces.toMap)

  def apply(variant: Variant): Board = Board(variant.pieces)

  def empty: Board = Board(Nil)
}
