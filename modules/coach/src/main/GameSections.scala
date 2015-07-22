package lila.coach

case class GameSections(
    all: GameSections.Section,
    opening: GameSections.Section,
    middle: GameSections.Section,
    end: GameSections.Section) {

  def aggregate(p: RichPov) = {
    copy(
      all = all.add(p.division.plies / 2, p.accuracy.map(_.all)),
      opening = opening.add(p.division.openingSize / 2, p.accuracy.map(_.opening)),
      middle = middle.add(~p.division.middleSize / 2, p.accuracy.flatMap(_.middle)),
      end = end.add(~p.division.endSize / 2, p.accuracy.flatMap(_.end))
    )
  }

  def merge(s: GameSections) = GameSections(
    all = all merge s.all,
    opening = opening merge s.opening,
    middle = middle merge s.middle,
    end = end merge s.end)
}

object GameSections {

  case class Section(moves: NbSum, acpl: NbSum) {
    def isEmpty = moves.nb == 0
    def add(m: Int, a: Option[Int]) =
      if (m == 0) this
      else copy(moves add m, a.fold(acpl)(acpl.add))

    def merge(s: Section) = Section(
      moves merge s.moves,
      acpl merge s.acpl)
  }
  val emptySection = Section(NbSum.empty, NbSum.empty)
  val empty = GameSections(emptySection, emptySection, emptySection, emptySection)
}
