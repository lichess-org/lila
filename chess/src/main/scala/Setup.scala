package lila.chess

import Variant._

object Setup {

  def apply(variant: Variant): Game = Game(
    board = Board(pieces = variant.pieces)
  )
}
