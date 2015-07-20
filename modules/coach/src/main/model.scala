package lila.coach

case class RichPov(
  pov: lila.game.Pov,
  initialFen: Option[String],
  analysis: Option[lila.analyse.Analysis],
  division: chess.Division,
  accuracy: Option[lila.analyse.Accuracy.DividedAccuracy])

case class OpeningFamily(name: String, codes: List[String])
case class OpeningFamilies(white: List[OpeningFamily], black: List[OpeningFamily])

case class OpeningApiData(
  results: Results,
  colorResults: ColorResults,
  openings: Openings,
  families: OpeningFamilies)
