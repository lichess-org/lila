package lidraughts.evalCache

import draughts.format.{ Forsyth, FEN }
import draughts.variant.Variant
import org.joda.time.DateTime
import scalaz.NonEmptyList

import lidraughts.tree.Eval.Score
import lidraughts.user.User

case class EvalCacheEntry(
    _id: EvalCacheEntry.Id,
    nbMoves: Int, // multipv cannot be greater than number of legal moves
    evals: List[EvalCacheEntry.Eval],
    usedAt: DateTime
) {

  import EvalCacheEntry._

  def id = _id

  def add(eval: Eval) = copy(
    evals = EvalCacheSelector(eval :: evals),
    usedAt = DateTime.now
  )

  // finds the best eval with at least multiPv pvs and the required nodecount,
  // and truncates its pvs to multiPv
  def makeBestMultiPvEval(multiPv: Int, minNodes: Int = 0): Option[Eval] =
    evals
      .find(e => e.knodes.intNodes >= minNodes && e.multiPv >= multiPv.atMost(nbMoves))
      .map(_ takePvs multiPv)

  def similarTo(other: EvalCacheEntry) =
    id == other.id && evals == other.evals
}

object EvalCacheEntry {

  val MIN_KNODES = 3000
  val MIN_DEPTH = 20
  val MIN_PV_SIZE = 6
  val MAX_PV_SIZE = 10
  val MAX_MULTI_PV = 5

  case class Eval(
      pvs: NonEmptyList[Pv],
      knodes: Knodes,
      depth: Int,
      by: User.ID,
      trust: Trust
  ) {

    def multiPv = pvs.size

    def bestPv: Pv = pvs.head

    def bestMove: String = bestPv.moves.value.head

    def looksValid = pvs.toList.forall(_.looksValid) && {
      pvs.toList.forall(_.score.winFound) || (knodes.value >= MIN_KNODES || depth >= MIN_DEPTH)
    }

    def truncatePvs = copy(pvs = pvs.map(_.truncate))

    def takePvs(multiPv: Int) = copy(
      pvs = NonEmptyList.nel(pvs.head, pvs.tail.take(multiPv - 1))
    )

    def depthAboveMin = (depth - MIN_DEPTH) atLeast 0
  }

  case class Knodes(value: Int) extends AnyVal {

    def intNodes: Int = {
      val nodes = value * 1000d
      if (nodes.toInt == nodes) nodes.toInt
      else if (nodes > 0) Integer.MAX_VALUE
      else Integer.MIN_VALUE
    }
  }

  case class Pv(score: Score, moves: Moves) {

    def looksValid = score.win match {
      case None => moves.value.size > MIN_PV_SIZE
      case Some(win) => win.value != 0 // sometimes we get #0. Dunno why.
    }

    def truncate = copy(moves = moves.truncate)
  }

  case class Moves(value: NonEmptyList[String]) extends AnyVal {

    def truncate = copy(value = NonEmptyList.nel(value.head, value.tail.take(MAX_PV_SIZE - 1)))
  }

  case class Trust(value: Double) extends AnyVal {
    def isTooLow = value <= 0
  }

  case class TrustedUser(trust: Trust, user: User)

  final class SmallFen private (val value: String) extends AnyVal with StringValue

  object SmallFen {
    private[evalCache] def raw(str: String) = new SmallFen(str)
    def make(variant: Variant, fen: FEN): SmallFen = {
      val base = Forsyth.<<@(variant, fen.value).fold(fen.value.split(':').take(3).mkString("").filter { c => c != 'W' }) { sit =>
        val boardStr = Forsyth.compressedBoard(sit.board)
        sit.color.fold(boardStr, "0" + boardStr)
      }
      val str = if (variant.frisianVariant) base + ~fen.value.split(':').lift(5) else base
      new SmallFen(str)
    }
    def validate(variant: Variant, fen: FEN): Option[SmallFen] =
      Forsyth.<<@(variant, fen.value).exists(_ playable false) option make(variant, fen)
  }

  case class Id(variant: Variant, smallFen: SmallFen)

  case class Input(id: Id, fen: FEN, eval: Eval)

  object Input {
    case class Candidate(variant: Variant, fen: String, eval: Eval) {
      def input = SmallFen.validate(variant, FEN(fen)) ifTrue eval.looksValid map { smallFen =>
        Input(Id(variant, smallFen), FEN(fen), eval.truncatePvs)
      }
    }
  }
}
