package shogi

import scala.collection.mutable.ArrayBuffer

import shogi.format.usi.Usi

final case class MoveActor(
    piece: Piece,
    pos: Pos,
    situation: Situation
) {

  lazy val destinations: List[Pos] = situation.variant.kingSafetyFilter(this)

  // Destinations without taking defending the king into account
  def unsafeDestinations: List[Pos] =
    shortRange(piece.directDirs) ::: longRange(piece.projectionDirs)

  def toUsis: List[Usi.Move] = {
    val normalMoves = destinations
      .withFilter(!situation.variant.pieceInDeadZone(piece, _))
      .map(Usi.Move(pos, _, false))
    val promotedMoves = destinations
      .withFilter(situation.variant.canPromote(piece, pos, _))
      .map(Usi.Move(pos, _, true))

    normalMoves ::: promotedMoves
  }

  def color        = piece.color
  def is(c: Color) = c == piece.color

  private def shortRange(dirs: Directions): List[Pos] =
    dirs flatMap { _(pos) } filter { to => 
      situation.variant.isInsideBoard(to) && situation.board.pieces.get(to).fold(true)(_.color != color)
    }

  private def longRange(dirs: Directions): List[Pos] = {
    val buf = new ArrayBuffer[Pos]
    @scala.annotation.tailrec
    def addAll(p: Pos, dir: Direction): Unit = {
      dir(p) match {
        case Some(to) if situation.variant.isInsideBoard(to) =>
          situation.board.pieces.get(to) match {
            case None => {
              buf += to
              addAll(to, dir)
            }
            case Some(piece) =>
              if (piece.color != color)
                buf += to
          }
        case _ => ()
      }
    }

    dirs foreach { addAll(pos, _) }
    buf.toList
  }
}
