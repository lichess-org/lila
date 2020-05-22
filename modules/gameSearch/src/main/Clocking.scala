package lila.gameSearch

case class Clocking(
    initMin: Option[Int] = None,
    initMax: Option[Int] = None,
    incMin: Option[Int] = None,
    incMax: Option[Int] = None
) {

  def nonEmpty = initMin.isDefined || initMax.isDefined || incMin.isDefined || incMax.isDefined
}
