package shogi

object Setup {

  def apply(variant: shogi.variant.Variant) = Game(variant)
}
