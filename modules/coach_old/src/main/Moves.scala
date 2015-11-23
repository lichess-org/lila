package lila.coach

case class Move(nb: Int, acpl: NbSum, time: NbSum) {

  def merge(m: Move) = Move(
    nb = nb + m.nb,
    acpl = acpl merge m.acpl,
    time = time merge m.time)

  def add(a: Option[Int], t: Option[Int]) = copy(
    nb = nb + 1,
    acpl = a.fold(acpl)(acpl.add),
    time = t.fold(time)(time.add))
}
object Move {
  val empty = Move(0, NbSum.empty, NbSum.empty)
}

case class TrimmedMoves(moves: Vector[Move]) {

  def fill = FilledMoves {
    moves ++ Vector.fill(Moves.SIZE - moves.size)(Move.empty)
  }

  def aggregate(p: RichPov) = fill.aggregate(p).trim

  def merge(o: TrimmedMoves) = fill.merge(o.fill).trim
}

case class FilledMoves(moves: Vector[Move]) {

  def trim = TrimmedMoves {
    moves.takeWhile(_.nb > 0)
  }

  def aggregate(p: RichPov) = FilledMoves {
    val moveTimes = p.pov.game.hasClock option p.pov.game.moveTimes(p.pov.color).toVector
    val accuracy = p.moveAccuracy.map(_.toVector)

    (0 to (Moves.SIZE - 1).min(p.pov.game.playerMoves(p.pov.color) - 1)).foldLeft(moves) {
      case (moves, index) => moves.updated(
        index,
        moves(index).add(
          accuracy.flatMap(_ lift index),
          if (index == 0) Some(0) else moveTimes.flatMap(_ lift index)))
    }
  }

  // TODO keep longest size
  def merge(o: FilledMoves) = FilledMoves {
    moves zip o.moves map { case (a, b) => a merge b }
  }
}
object Moves {
  val SIZE = 60
  val empty = TrimmedMoves(Vector.empty)
}

case class ColorMoves(white: TrimmedMoves, black: TrimmedMoves) {

  def aggregate(p: RichPov) = copy(
    white = if (p.pov.color.white) white aggregate p else white,
    black = if (p.pov.color.black) black aggregate p else black)

  def merge(o: ColorMoves) = ColorMoves(
    white = white merge o.white,
    black = black merge o.black)
}

object ColorMoves {

  val empty = ColorMoves(Moves.empty, Moves.empty)
}
