package lidraughts.draughtsnet

import draughts.format.pdn.Dumper
import draughts.format.Uci
import draughts.{ Replay, Move, Situation }
import scalaz.Validation.success
import scalaz.Validation.FlatMap._

import lidraughts.analyse.{ Analysis, Info, PdnMove }
import lidraughts.base.LidraughtsException

// convert variations from UCI to PDN.
// also drops extra variations
private object UciToPdn {

  type WithErrors[A] = (A, List[Exception])

  def apply(replay: Replay, analysis: Analysis): WithErrors[Analysis] = {

    val pliesWithAdviceAndVariation: Set[Int] = analysis.advices.collect {
      case a if a.info.hasVariation => a.ply
    }(scala.collection.breakOut)

    val onlyMeaningfulVariations: List[Info] = analysis.infos map { info =>
      if (pliesWithAdviceAndVariation(info.ply)) info
      else info.dropVariation
    }

    def uciToPdn(ply: Int, variation: List[String]): Valid[List[PdnMove]] = for {
      situation ← if (ply == replay.setup.startedAtTurn + 1) success(replay.setup.situation)
      else replay moveAtPly ply map (_.situationBefore) toValid "No move found"
      ucis ← variation.map(Uci.apply).sequence toValid "Invalid UCI moves " + variation
      moves ← ucis.foldLeft[Valid[(Situation, List[Move])]](success(situation -> Nil)) {
        case (scalaz.Success((sit, moves)), uci: Uci.Move) =>
          sit.move(uci.orig, uci.dest, uci.promotion) prefixFailuresWith s"ply $ply " map { move =>
            move.situationAfter -> (move :: moves)
          }
        case (failure, _) => failure
      }
    } yield moves._2.reverse map Dumper.apply

    onlyMeaningfulVariations.foldLeft[WithErrors[List[Info]]]((Nil, Nil)) {
      case ((infos, errs), info) if info.variation.isEmpty => (info :: infos, errs)
      case ((infos, errs), info) => uciToPdn(info.ply, info.variation).fold(
        err => (info.dropVariation :: infos, LidraughtsException(err) :: errs),
        pdn => (info.copy(variation = pdn) :: infos, errs)
      )
    } match {
      case (infos, errors) => analysis.copy(infos = infos.reverse) -> errors
    }
  }
}
