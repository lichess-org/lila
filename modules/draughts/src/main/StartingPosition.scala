package draughts

import variant.{ Variant, Standard }

case class StartingPosition(
    code: String,
    fen: String,
    moves: String,
    name: Option[String] = None,
    wikiPath: Option[String] = None,
    featurable: Boolean = true
) {

  val shortName = name.fold(code) { n => s"$code - ${n takeWhile (':'!=)}" }
  val fullName = name.fold(code) { n => s"$code - $n" }

  def url = wikiPath.map(u => s"https://en.wikipedia.org/wiki/$u")
  def initialStandard = fen == Standard.initialFen
  def initialVariant(v: Variant) = fen == v.initialFen
}

object StartingPosition {

  case class Category(name: String, positions: List[StartingPosition])

  /*lazy val featurable = new scala.util.Random(475591).shuffle(all.filter(_.featurable)).toIndexedSeq

  def randomFeaturable = featurable(scala.util.Random.nextInt(featurable.size))*/

}
