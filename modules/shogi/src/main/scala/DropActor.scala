package shogi

import shogi.format.usi.Usi

final case class DropActor(
    piece: Piece,
    situation: Situation
) {

  lazy val destinations: List[Pos] =
    if (situation.variant.supportsDrops)
      situation.variant.dropLegalityFilter(this)
    else Nil

  def toUsis: List[Usi.Drop] =
    destinations.map(Usi.Drop(piece.role, _))

  def color = piece.color

}

object DropActor {

  def blockades(sit: Situation, kingPos: Pos): List[Pos] = {
    def attacker(piece: Piece, from: Pos) =
      piece.projectionDirs.nonEmpty && piece.eyes(from, kingPos) && piece.color != sit.color
    @scala.annotation.tailrec
    def forward(p: Pos, dir: Direction, squares: List[Pos]): List[Pos] =
      dir(p) match {
        case None                                                    => Nil
        case Some(next) if !sit.variant.isInsideBoard(next)          => Nil
        case Some(next) if sit.board(next).exists(attacker(_, next)) => next :: squares
        case Some(next) if sit.board(next).isDefined                 => Nil
        case Some(next)                                              => forward(next, dir, next :: squares)
      }
    Pos.allDirections flatMap { forward(kingPos, _, Nil) } filter { square =>
      sit.board.place(Piece(sit.color, Gold), square) exists { defended =>
        !sit.copy(board = defended).check
      }
    }
  }

}
