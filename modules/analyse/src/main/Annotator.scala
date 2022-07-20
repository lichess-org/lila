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
            // variations = makeVariation(advice.ply, advice) :: Nil
          )
      )
    }
}
