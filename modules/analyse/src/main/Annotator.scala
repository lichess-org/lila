package lila.analyse

import shogi.format.{ Glyphs, Notation, Tag }
import shogi.opening._
import shogi.{ Color, Status }

final class Annotator {

  def apply(
      k: Notation,
      analysis: Option[Analysis]
  ): Notation =
    annotateMoves(
      k,
      analysis ?? (_.advices)
    )

  private def annotateMoves(k: Notation, advices: List[Advice]): Notation =
    advices.foldLeft(k) { case (kif, advice) =>
      kif.updatePly(
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
