package chess

object Setup {

  def apply(variant: chess.variant.Variant) = Game(variant)
}
