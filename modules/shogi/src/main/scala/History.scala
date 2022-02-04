package shogi

import shogi.format.usi.Usi

case class History(
    lastMove: Option[Usi],
    positionHashes: PositionHash
) {

  private def isRepetition(times: Int) =
    positionHashes.length > (times - 1) * 4 * Hash.size && {
      // compare only hashes for positions with the same side to move
      val positions = positionHashes.sliding(Hash.size, 2 * Hash.size).toList
      positions.headOption match {
        case Some(Array(x, y, z)) =>
          (positions count {
            case Array(x2, y2, z2) => x == x2 && y == y2 && z == z2
            case _                 => false
          }) >= times
        case _ => times <= 1
      }
    }

  lazy val fourfoldRepetition = isRepetition(4)

  def withLastMove(u: Usi) = copy(lastMove = Some(u))

  def withPositionHashes(h: PositionHash) = copy(positionHashes = h)

  override def toString = {
    val positions = (positionHashes grouped Hash.size).toList
    s"${lastMove.fold("-")(_.usi)} ${positions.map(Hash.debug).mkString(" ")}"
  }
}

object History {

  def empty: History = History(None, Array.empty)

  def apply(
      lastMove: Option[String]
  ): History =
    History(
      lastMove = lastMove flatMap Usi.apply,
      positionHashes = Array.empty
    )
}
