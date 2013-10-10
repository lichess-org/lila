package lila.analyse

import chess.format.pgn.Dumper
import chess.format.UciMove
import chess.{ Replay, Move }
import scalaz.{ NonEmptyList, Success }

import lila.common.LilaException

// convert variations from UCI to PGN.
// also drops extra variations
private[analyse] object UciToPgn {

  type WithErrors[A] = (A, List[Exception])

  def apply(replay: Replay, analysis: Analysis): WithErrors[Analysis] = {

    val plySet = (analysis.infoAdvices.pp collect {
      case (info, Some(_)) ⇒ info.ply
    }).toSet

    val onlyMeaningfulVariations: List[Info] = analysis.infos map { info ⇒
      plySet(info.ply).fold(info, info.dropVariation)
    }

    def uciToPgn(ply: Int, variation: List[String]): Valid[List[PgnMove]] = for {
      fromMove ← replay.chronoMoves lift (ply - 2) toValid "No move found"
      ucis ← variation.map(UciMove.apply).sequence toValid "Invalid UCI moves " + variation
      moves ← ucis.foldLeft[Valid[NonEmptyList[Move]]](success(NonEmptyList(fromMove))) {
        case (Success(moves), uci) ⇒
          moves.head.situationAfter.move(uci.orig, uci.dest, uci.promotion) map { move ⇒
            move <:: moves
          }
        case (failure, _) ⇒ failure
      }
    } yield moves.reverse.tail map Dumper.apply 

    onlyMeaningfulVariations.foldLeft[WithErrors[List[Info]]]((Nil, Nil)) {
      case ((infos, errs), info) if info.variation.isEmpty ⇒ (info :: infos, errs)
      case ((infos, errs), info) ⇒ uciToPgn(info.ply, info.variation).fold(
        err ⇒ (info.dropVariation :: infos, LilaException(err) :: errs),
        pgn ⇒ (info.copy(variation = pgn) :: infos, errs)
      )
    } match {
      case (infos, errors) ⇒ analysis.copy(infos = infos.reverse) -> errors
    }
  }
}
