package chess
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
}
