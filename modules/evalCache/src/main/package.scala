package lila.evalCache

import cats.data.NonEmptyList
import chess.format.{ Uci, Fen }
import chess.variant.Variant

export lila.Lila.{ *, given }

private val logger = lila.log("evalCache")

val MIN_KNODES   = 3000
val MIN_DEPTH    = 20
val MIN_PV_SIZE  = 6
val MAX_PV_SIZE  = 10
val MAX_MULTI_PV = 5

opaque type Knodes = Int
object Knodes extends OpaqueInt[Knodes]:
  extension (a: Knodes)
    def intNodes: Int =
      val nodes = a.value * 1000d
      if (nodes.toInt == nodes) nodes.toInt
      else if (nodes > 0) Integer.MAX_VALUE
      else Integer.MIN_VALUE

opaque type Moves = NonEmptyList[Uci]
object Moves extends TotalWrapper[Moves, NonEmptyList[Uci]]:
  extension (a: Moves) def truncate: Moves = NonEmptyList(a.value.head, a.value.tail.take(MAX_PV_SIZE - 1))

opaque type Trust = Double
object Trust extends OpaqueDouble[Trust]:
  extension (a: Trust)
    inline def isTooLow = a.value <= 0
    inline def isEnough = !a.isTooLow

opaque type SmallFen = String
object SmallFen extends OpaqueString[SmallFen]:
  def make(variant: Variant, fen: Fen.Simple): SmallFen =
    val base = fen.value.split(' ').take(4).mkString("").filter { c =>
      c != '/' && c != '-' && c != 'w'
    }
    val str = variant match
      case chess.variant.ThreeCheck => base + ~fen.value.split(' ').lift(6)
      case _                        => base
    SmallFen(str)
  def validate(variant: Variant, fen: Fen.Epd): Option[SmallFen] =
    Fen.read(variant, fen).exists(_ playable false) option make(variant, fen.simple)
