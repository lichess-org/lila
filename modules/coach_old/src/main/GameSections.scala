package lila.coach

case class GameSections(
    all: GameSections.Section,
    opening: GameSections.Section,
    middle: GameSections.Section,
    end: GameSections.Section) {

  private def avg(ints: Vector[Int]) = ints.nonEmpty option (ints.sum / ints.size)
  private def boundedAvg(ints: Option[Vector[Int]], bounds: (Int, Int)) = ints ?? { is =>
    bounds match {
      case (from, to) => avg(is drop from take (to - from))
    }
  }

  def aggregate(p: RichPov) = {
    val moveTimes = p.pov.game.hasClock option p.pov.game.moveTimes(p.pov.color).toVector
    copy(
      all = all.add(
        m = p.division.plies / 2,
        a = p.accuracy.map(_.all),
        t = moveTimes.flatMap(avg)),
      opening = opening.add(
        m = p.division.openingSize / 2,
        a = p.accuracy.map(_.opening),
        t = p.division.openingBounds.flatMap { boundedAvg(moveTimes, _) }),
      middle = middle.add(
        m = ~p.division.middleSize / 2,
        a = p.accuracy.flatMap(_.middle),
        t = p.division.middleBounds.flatMap { boundedAvg(moveTimes, _) }),
      end = end.add(
        m = ~p.division.endSize / 2,
        a = p.accuracy.flatMap(_.end),
        t = p.division.endBounds.flatMap { boundedAvg(moveTimes, _) })
    )
  }

  def merge(s: GameSections) = GameSections(
    all = all merge s.all,
    opening = opening merge s.opening,
    middle = middle merge s.middle,
    end = end merge s.end)
}

object GameSections {

  case class Section(
      nb: Int,
      moves: NbSum,
      acpl: NbSum,
      time: NbSum) {

    def merge(m: Section) = Section(
      nb = nb + m.nb,
      moves = moves merge m.moves,
      acpl = acpl merge m.acpl,
      time = time merge m.time)

    def add(m: Int, a: Option[Int], t: Option[Int]) = copy(
      nb = nb + 1,
      moves = moves add m,
      acpl = a.fold(acpl)(acpl.add),
      time = t.fold(time)(time.add))
  }
  object Section {
    val empty = Section(0, NbSum.empty, NbSum.empty, NbSum.empty)
  }

  val empty = GameSections(Section.empty, Section.empty, Section.empty, Section.empty)
}
