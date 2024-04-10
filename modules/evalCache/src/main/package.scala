package lila.evalCache

import chess.format.{ Fen, Uci }
import chess.variant.Variant

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
import lila.tree.CloudEval
import lila.core.chess.MultiPv

extension (e: CloudEval)
  def multiPv = MultiPv(e.pvs.size)
  def takePvs(multiPv: MultiPv) =
    e.copy(pvs = NonEmptyList(e.pvs.head, e.pvs.tail.take(e.multiPv.value - 1)))

opaque type SmallFen = String
object SmallFen extends OpaqueString[SmallFen]:
  def make(variant: Variant, fen: Fen.Simple): SmallFen =
    val base = fen.value.split(' ').take(4).mkString("").filter { c =>
      c != '/' && c != '-' && c != 'w'
    }
    variant match
      case chess.variant.ThreeCheck => base + ~fen.value.split(' ').lift(6)
      case _                        => base
