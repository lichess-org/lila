package lila.tree

import chess.format.Uci
import chess.Position
import chess.eval.{ Eval as Ev, * }

case class Eval(cp: Option[Ev.Cp], mate: Option[Ev.Mate], best: Option[Uci]):

  def isEmpty = cp.isEmpty && mate.isEmpty

  def dropBest = copy(best = None)

  def invert = copy(cp = cp.map(_.invert), mate = mate.map(_.invert))

  def score: Option[Score] = cp.map(Score.Cp(_)).orElse(mate.map(Score.Mate(_)))

  def forceAsCp: Option[Ev.Cp] = cp.orElse(mate.map {
    case m if m.negative => Ev.Cp(Int.MinValue - m.value)
    case m => Ev.Cp(Int.MaxValue - m.value)
  })

object evals:
  val initial = Eval(Some(Ev.Cp.initial), None, None)
  val empty = Eval(None, None, None)
  def fromScore(score: Score) = Eval(score.cp, score.mate, None)

  import play.api.libs.json.*
  import scalalib.json.Json.given
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
  type GetSinglePvEval = Position => Fu[Option[CloudEval]]
