package lila.evalCache

import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi
import shogi.variant.Variant
import org.joda.time.DateTime
import cats.data.NonEmptyList

import lila.tree.Eval.Score
import lila.user.User

case class EvalCacheEntry(
    _id: EvalCacheEntry.Id,
    nbMoves: Int, // multipv cannot be greater than number of legal moves
    evals: List[EvalCacheEntry.Eval],
    usedAt: DateTime
) {

  import EvalCacheEntry._

  def id = _id

  def add(eval: Eval) =
    copy(
      evals = EvalCacheSelector(eval :: evals),
      usedAt = DateTime.now
    )

  // finds the best eval with at least multiPv pvs,
  // and truncates its pvs to multiPv
  def makeBestMultiPvEval(multiPv: Int): Option[Eval] =
    evals
      .find(_.multiPv >= multiPv.atMost(nbMoves))
      .map(_ takePvs multiPv)

  def similarTo(other: EvalCacheEntry) =
    id == other.id && evals == other.evals
}

object EvalCacheEntry {

  val MIN_KNODES   = 3000
  val MIN_DEPTH    = 20
  val MIN_PV_SIZE  = 6
  val MAX_PV_SIZE  = 10
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

    def bestMove: Usi = bestPv.moves.value.head

    def looksValid =
      pvs.toList.forall(_.looksValid) && {
        pvs.toList.forall(_.score.mateFound) || (knodes.value >= MIN_KNODES || depth >= MIN_DEPTH)
      }

    def truncatePvs = copy(pvs = pvs.map(_.truncate))

    def takePvs(multiPv: Int) =
      copy(
        pvs = NonEmptyList(pvs.head, pvs.tail.take(multiPv - 1))
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

    def looksValid =
      score.mate match {
        case None       => moves.value.size > MIN_PV_SIZE
        case Some(mate) => mate.value != 0 // sometimes we get #0. Dunno why.
      }

    def truncate = copy(moves = moves.truncate)
  }

  case class Moves(value: NonEmptyList[Usi]) extends AnyVal {

    def truncate = copy(value = NonEmptyList(value.head, value.tail.take(MAX_PV_SIZE - 1)))
  }

  case class Trust(value: Double) extends AnyVal {
    def isTooLow = value <= 0
    def isEnough = !isTooLow
  }

  case class TrustedUser(trust: Trust, user: User)

  final class SmallSfen private (val value: String) extends AnyVal with StringValue

  object SmallSfen {
    private[evalCache] def raw(str: String) = new SmallSfen(str)
    def make(sfen: Sfen): SmallSfen =
      new SmallSfen(sfen.truncate.value.filterNot(_ == '/'))
    def validate(variant: Variant, sfen: Sfen): Option[SmallSfen] =
      sfen.toSituation(variant).exists(_.playable(false, false)) option make(sfen)
  }

  case class Id(variant: Variant, smallSfen: SmallSfen)

  case class Input(id: Id, sfen: Sfen, eval: Eval)

  object Input {
    case class Candidate(variant: Variant, sfen: String, eval: Eval) {
      def input =
        SmallSfen.validate(variant, Sfen(sfen)) ifTrue eval.looksValid map { smallSfen =>
          Input(Id(variant, smallSfen), Sfen(sfen), eval.truncatePvs)
        }
    }
  }
}
