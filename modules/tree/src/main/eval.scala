package lila.tree

import chess.format.Uci
import chess.{ Color, Position }
import chess.eval.{ Eval as Ev, * }

extension (score: Score)
  def forceAsCp: Ev.Cp =
    score.fold(
      identity,
      m => if m.positive then Ev.Cp(Int.MaxValue - m.value) else Ev.Cp(Int.MinValue - m.value),
      Ev.Cp(Int.MaxValue)
    )

case class Eval(score: Option[WhiteScore], best: Option[Uci]):

  def cp: Option[Ev.Cp] = score.flatMap(_.white.cp)
  def mate: Option[Ev.Mate] = score.flatMap(_.white.mate)
  def forceAsCp: Option[Ev.Cp] = score.map(_.white.forceAsCp)

  def pov(color: Color): Option[Score] = score.map(_.pov(color))

  def isEmpty = score.isEmpty

  def isGameOver = score.exists(_.isGameOver)

  def dropBest = copy(best = None)

object evals:
  val initial = Eval(Some(WhiteScore.initial), None)
  val empty = Eval(None, None)
  def fromScore(score: WhiteScore) = Eval(Some(score), None)

  import play.api.libs.json.*
  import scalalib.json.Json.given
  import chess.json.Json.given

  def json(eval: Eval): Option[JsObject] =
    eval.score
      .filterNot(_.isGameOver)
      .map: score =>
        Json
          .obj()
          .add("cp" -> score.white.cp)
          .add("mate" -> score.white.mate)
          .add("best" -> eval.best)

opaque type Moves = NonEmptyList[Uci]
object Moves extends TotalWrapper[Moves, NonEmptyList[Uci]]

opaque type Knodes = Int
object Knodes extends OpaqueInt[Knodes]:
  extension (a: Knodes)
    def intNodes: Int =
      val nodes = a.value * 1000d
      if nodes.toInt == nodes then nodes.toInt
      else Integer.MAX_VALUE

case class Pv(score: WhiteScore, moves: Moves)

case class CloudEval(pvs: NonEmptyList[Pv], knodes: Knodes, depth: lila.core.chess.Depth):
  def isGameOver = pvs.exists(_.score.isGameOver)

object CloudEval:
  type GetSinglePvEval = Position => Fu[Option[CloudEval]]
