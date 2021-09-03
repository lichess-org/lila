package lila.analyse

import shogi.format.pgn.{ Glyphs, KifMove, Tag, Kif }
import shogi.opening._
import shogi.{ Color, Status }

final class Annotator(netDomain: lila.common.config.NetDomain) {

  def apply(
      k: Kif,
      analysis: Option[Analysis],
      opening: Option[FullOpening.AtPly],
      winner: Option[Color],
      status: Status
  ): Kif =
    annotateStatus(winner, status) {
      annotateOpening(opening) {
        annotateMoves(k, analysis ?? (_.advices))
      }.copy(
        tags = k.tags + Tag(_.Annotator, netDomain)
      )
    }

  private def annotateStatus(winner: Option[Color], status: Status)(k: Kif) =
    lila.game.StatusText(status, winner, shogi.variant.Standard) match {
      case ""   => k
      case text => k.updateLastPly(_.copy(result = text.some))
    }

  private def annotateOpening(opening: Option[FullOpening.AtPly])(k: Kif) =
    opening.fold(k) { o =>
      k.updatePly(o.ply, _.copy(opening = o.opening.ecoName.some))
    }

  private def annotateMoves(k: Kif, advices: List[Advice]): Kif =
    advices.foldLeft(k) { case (kif, advice) =>
      kif.updatePly(
        advice.ply,
        move =>
          move.copy(
            comments = advice.makeComment(true, true) :: move.comments,
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
