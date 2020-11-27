package chess
package variant

import scalaz.Validation.FlatMap._

case object Standard
    extends Variant(
      id = 1,
      key = "standard",
      name = "Standard",
      shortName = "Std",
      title = "Standard rules of shogi",
      standardInitialPosition = false
    ) {
  
  val pieces: Map[Pos, Piece] = Variant.symmetricRank(backRank, backRank2)
}

