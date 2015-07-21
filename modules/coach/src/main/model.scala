package lila.coach

case class RichPov(
  pov: lila.game.Pov,
  initialFen: Option[String],
  analysis: Option[lila.analyse.Analysis],
  division: chess.Division,
  accuracy: Option[lila.analyse.Accuracy.DividedAccuracy])

case class OpeningFamily(firstMove: String, families: List[String])
