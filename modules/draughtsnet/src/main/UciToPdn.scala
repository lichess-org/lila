package lidraughts.draughtsnet

import draughts.format.pdn.{ Dumper, Std }
import draughts.format.Uci
import draughts.{ Move, Replay, Situation }
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

    def mk(sit: Situation, ucis: List[Uci], moves: List[Move], ambs: List[(Uci, String)]): Option[(Situation, List[Move])] = {
      var newAmb = none[(Uci, String)]
      val res = ucis match {
        case uci :: rest =>
          sit.move(
            uci.origDest._1, uci.origDest._2, none,
            finalSquare = true,
            if (ambs.isEmpty) None else ambs.collect({ case (ambFrom, ambUci) if ambFrom == uci => ambUci }).some
          ) match {
              case scalaz.Success(move) =>
                if (move.capture.fold(false)(_.lengthCompare(1) > 0) && move.situationBefore.ambiguitiesMove(move) > 0)
                  newAmb = (uci -> move.toUci.uci).some
                mk(move.situationAfter, rest, move :: moves, if (newAmb.isDefined) newAmb.get :: ambs else ambs)
              case _ => none
            }
        case _ => (sit, moves).some
      }
      res match {
        case None if newAmb.isDefined => mk(sit, ucis, moves, newAmb.get :: ambs)
        case _ => res
      }
    }

    def uciToPdn(ply: Int, variation: List[String]): Valid[List[PdnMove]] = for {
      situation ← if (ply == replay.setup.startedAtTurn + 1) success(replay.setup.situation)
      else replay moveAtPly ply map (_.situationBefore) toValid "No move found"
      ucis ← variation.map(Uci.apply).sequence toValid "Invalid UCI moves " + variation
      moves ← mk(situation, ucis, Nil, Nil) toValid "Invalid variation " + variation
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
