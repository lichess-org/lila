package shogi
package variant

case object FromPosition
    extends Variant(
      id = 3,
      key = "fromPosition",
      name = "From Position",
      shortName = "FEN",
      title = "Custom starting position",
      standardInitialPosition = false
    ) {

  def pieces = Standard.pieces
  def hand   = Standard.hand

  def numberOfRanks: Int = Standard.numberOfRanks
  def numberOfFiles: Int = Standard.numberOfFiles

  def allSquares = Standard.allSquares

  def promotionRanks(color: Color) = Standard promotionRanks color
}
