package lila.evalCache

import chess.format.{ Uci, Fen }
import chess.variant.Variant

export lila.Lila.{ *, given }

opaque type Knodes = Int
object Knodes extends OpaqueInt[Knodes]:
  extension (a: Knodes)
    def intNodes: Int =
      val nodes = a.value * 1000d
      if nodes.toInt == nodes then nodes.toInt
      else Integer.MAX_VALUE

opaque type Moves = NonEmptyList[Uci]
object Moves extends TotalWrapper[Moves, NonEmptyList[Uci]]

opaque type SmallFen = String
object SmallFen extends OpaqueString[SmallFen]:
  def make(variant: Variant, fen: Fen.Simple): SmallFen =
    val base = fen.value.split(' ').take(4).mkString("").filter { c =>
      c != '/' && c != '-' && c != 'w'
    }
    variant match
      case chess.variant.ThreeCheck => base + ~fen.value.split(' ').lift(6)
      case _                        => base
