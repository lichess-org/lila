package shogi

import format.usi.Usi

// Checks received by the respective side.
case class CheckCount(sente: Int = 0, gote: Int = 0) {
  def add(color: Color) =
    copy(
      sente = sente + color.fold(1, 0),
      gote = gote + color.fold(0, 1)
    )

  def nonEmpty = sente > 0 || gote > 0

  def apply(color: Color) = color.fold(sente, gote)

  def reset(color: Color) = {
    if (color.sente) {
      copy(sente = 0)
    } else {
      copy(gote = 0)
    }
  }
}

case class History(
    lastMove: Option[Usi] = None,
    positionHashes: PositionHash = Array.empty,
    checkCount: CheckCount = CheckCount(0, 0)
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

  def fourfoldRepetition = isRepetition(4)

  def perpetualCheck =
    fourfoldRepetition && (checkCount.sente >= 4 || checkCount.gote >= 4)

  def withLastMove(m: Usi) = copy(lastMove = Option(m))

  def withCheck(color: Color, v: Boolean) =
    if (v) copy(checkCount = checkCount add color) else copy(checkCount = checkCount reset color)

  def withCheckCount(cc: CheckCount) = copy(checkCount = cc)

  override def toString = {
    val positions = (positionHashes grouped Hash.size).toList
    s"${lastMove.fold("-")(_.usi)} ${positions.map(Hash.debug).mkString(" ")}"
  }
}

object History {

  def make(
      lastMove: Option[String] // a2a4
  ): History =
    History(
      lastMove = lastMove flatMap Usi.apply,
      positionHashes = Array()
    )
}
