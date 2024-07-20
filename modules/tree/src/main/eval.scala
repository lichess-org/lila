package lila.tree

import cats.data.NonEmptyList
import chess.format.{ Fen, Uci }
import chess.variant.Variant

case class Eval(cp: Option[Eval.Cp], mate: Option[Eval.Mate], best: Option[Uci]):

  def isEmpty = cp.isEmpty && mate.isEmpty

  def dropBest = copy(best = None)

  def invert = copy(cp = cp.map(_.invert), mate = mate.map(_.invert))

  def score: Option[Score] = cp.map(Score.Cp(_)).orElse(mate.map(Score.Mate(_)))

  def forceAsCp: Option[Eval.Cp] = cp.orElse(mate.map {
    case m if m.negative => Eval.Cp(Int.MinValue - m.value)
    case m               => Eval.Cp(Int.MaxValue - m.value)
  })

enum Score:
  case Cp(c: Eval.Cp)
  case Mate(m: Eval.Mate)

  inline def fold[A](w: Eval.Cp => A, b: Eval.Mate => A): A = this match
    case Cp(cp)     => w(cp)
    case Mate(mate) => b(mate)

  inline def cp: Option[Eval.Cp]     = fold(Some(_), _ => None)
  inline def mate: Option[Eval.Mate] = fold(_ => None, Some(_))

  inline def isCheckmate = mate.exists(_.value == 0)
  inline def mateFound   = mate.isDefined

  inline def invert: Score                  = fold(c => Cp(c.invert), m => Mate(m.invert))
  inline def invertIf(cond: Boolean): Score = if cond then invert else this

  def eval: Eval = Eval(cp, mate, None)

object Score:
  def cp(cp: Int): Score     = Cp(Eval.Cp(cp))
  def mate(mate: Int): Score = Mate(Eval.Mate(mate))

object Eval:

  opaque type Cp = Int
  object Cp extends OpaqueInt[Cp]:
    val CEILING                               = Cp(1000)
    val initial                               = Cp(15)
    inline def ceilingWithSignum(signum: Int) = CEILING.invertIf(signum < 0)

    extension (cp: Cp)
      inline def centipawns = cp.value

      inline def pawns: Float      = cp.value / 100f
      inline def showPawns: String = "%.2f".format(pawns)

      inline def ceiled: Cp =
        if cp.value > Cp.CEILING then Cp.CEILING
        else if cp.value < -Cp.CEILING then -Cp.CEILING
        else cp

      inline def invert: Cp                  = Cp(-cp.value)
      inline def invertIf(cond: Boolean): Cp = if cond then invert else cp

      def signum: Int = Math.signum(cp.value.toFloat).toInt

  end Cp

  opaque type Mate = Int
  object Mate extends OpaqueInt[Mate]:
    extension (mate: Mate)
      inline def moves: Int = mate.value

      inline def invert: Mate                  = Mate(-moves)
      inline def invertIf(cond: Boolean): Mate = if cond then invert else mate

      inline def signum: Int = if positive then 1 else -1

      inline def positive = mate.value > 0
      inline def negative = mate.value < 0

  val initial = Eval(Some(Cp.initial), None, None)

  val empty = Eval(None, None, None)

  import play.api.libs.json.*
  import scalalib.json.Json.{ *, given }
  import chess.json.Json.given

  given jsonWrites: Writes[Eval] = Json.writes[Eval]

opaque type Moves = NonEmptyList[Uci]
object Moves extends TotalWrapper[Moves, NonEmptyList[Uci]]

opaque type Knodes = Int
object Knodes extends OpaqueInt[Knodes]:
  extension (a: Knodes)
    def intNodes: Int =
      val nodes = a.value * 1000d
      if nodes.toInt == nodes then nodes.toInt
      else Integer.MAX_VALUE

case class Pv(score: Score, moves: Moves)

case class CloudEval(pvs: NonEmptyList[Pv], knodes: Knodes, depth: lila.core.chess.Depth, by: UserId)

object CloudEval:
  type GetSinglePvEval = (Variant, Fen.Full) => Fu[Option[CloudEval]]
