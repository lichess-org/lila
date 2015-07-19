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
}

object GameSections {

  case class Section(
      nb: Int,
      nbAnalysed: Int,
      moveSum: Int,
      acplSum: Int) {
    def empty = nb == 0
    def acplAvg = (nbAnalysed > 0) option (acplSum / nbAnalysed)
    def moveAvg = (nb > 0) option (moveSum / nb)
    def add(move: Int, acpl: Option[Int]) =
      if (move == 0) this
      else copy(
        nb = nb + 1,
        moveSum = moveSum + move,
        nbAnalysed = nbAnalysed + acpl.isDefined.fold(1, 0),
        acplSum = acplSum + ~acpl)
  }
  val emptySection = Section(0, 0, 0, 0)
  val empty = GameSections(emptySection, emptySection, emptySection, emptySection)
}
