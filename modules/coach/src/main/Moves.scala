package lila.coach

case class Move(nbGames: Int, acplSum: Int) {

  def acplAvg = (nbGames > 0) option (acplSum / nbGames)

  def merge(m: Move) = Move(
    nbGames = nbGames + m.nbGames,
    acplSum = acplSum + m.acplSum)

  def add(acpl: Int) = copy(nbGames + 1, acplSum + acpl)
}

case class Moves(white: Vector[Move], black: Vector[Move]) {

  private def aggregateSide(side: Vector[Move], p: RichPov): Vector[Move] =
    p.moveAccuracy.fold(side) {
      _.take(Moves.SIZE).zipWithIndex.foldLeft(side) {
        case (side, (cp, index)) => side.updated(index, side(index) add cp)
      }
    }

  def aggregate(p: RichPov) = copy(
    white = if (p.pov.color.white) aggregateSide(white, p) else white,
    black = if (p.pov.color.black) aggregateSide(black, p) else black)

  private def mergeMoves(l1: Vector[Move], l2: Vector[Move]) = l1.zip(l2).map {
    case (m1, m2) => m1 merge m2
  }

  def merge(o: Moves) = Moves(
    white = mergeMoves(white, o.white),
    black = mergeMoves(black, o.black))
}

object Moves {

  val SIZE = 80

  private val emptyMove = Move(0, 0)
  private val emptySideData: Vector[Move] = Vector.fill(SIZE)(emptyMove)

  val empty = Moves(emptySideData, emptySideData)
}
