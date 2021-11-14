package lila.analyse

import shogi.format.Notation

final class Annotator {

  def apply(
      n: Notation,
      analysis: Option[Analysis]
  ): Notation =
    annotateMoves(
      n,
      analysis ?? (_.advices)
    )

  private def annotateMoves(n: Notation, advices: List[Advice]): Notation =
    advices.foldLeft(n) { case (notation, advice) =>
      notation.updatePly(
        advice.ply,
        move =>
          move.copy(
            comments = advice.makeComment(true, true) :: move.comments
            //variations = makeVariation(advice.ply, advice) :: Nil
          )
      )
    }

  // We also need uci to export this properly, so nah for now
  //private def makeVariation(ply: Int, advice: Advice): List[Move] =
  //  (advice.info.variation take 20).zipWithIndex.map { case (san, index) =>
  //    KifMove(ply + index, san)
  //  }
}
