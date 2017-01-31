package lila.evalCache

import scala.math.sqrt

import EvalCacheEntry._

/**
 * selects the evals to store
 * for a given position
 */
object EvalCacheSelector {

  private type Evals = List[TrustedEval]

  def apply(evals: Evals): Evals =
    // first, let us group evals by multiPv
    evals.groupBy(_.eval.multiPv).toList
      // and sort the groups by multiPv, higher first
      .sortBy(-_._1).map(_._2)
      // then sort each group's evals, and keep only the best eval in each group
      .map(_ sortBy ranking).map(_.headOption).flatten
      // finally, remove superfluous evals
      .foldLeft(Nil: Evals) {
        case (acc, te) if acc.exists { a => makesObsolete(a.eval, te.eval) } => acc
        case (acc, te) => acc :+ te
      }

  private def greatTrust(t: Trust) = t.value >= 5

  private def ranking(te: TrustedEval): Double = -{
    // if well trusted, only rank on depth
    if (greatTrust(te.trust)) te.eval.depth * 1000
    // else, rank on trust, and tie on depth
    else te.trust.value + te.eval.depth / 1000
  }

  //     {multiPv:4,depth:30} makes {multiPv:2,depth:25} obsolete,
  // but {multiPv:2,depth:30} does not make {multiPv:4,depth:25} obsolete
  private def makesObsolete(a: Eval, b: Eval) =
    a.multiPv > b.multiPv && a.depth >= b.depth
}
