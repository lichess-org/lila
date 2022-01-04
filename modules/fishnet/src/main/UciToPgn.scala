package lila.fishnet

import cats.data.Validated
import cats.data.Validated.valid
import cats.implicits._

import shogi.format.usi.Usi
import shogi.{ Drop, Move, Replay, Situation }

import lila.analyse.{ Analysis, Info }
import lila.base.LilaException

// validated usi strings and drops extra variations
private object VariationValidation {

  type WithErrors[A] = (A, List[Exception])

  def apply(replay: Replay, analysis: Analysis): WithErrors[Analysis] = {

    val pliesWithAdviceAndVariation: Set[Int] = analysis.advices.view.collect {
      case a if a.info.hasVariation => a.ply
    } to Set

    val onlyMeaningfulVariations: List[Info] = analysis.infos map { info =>
      if (pliesWithAdviceAndVariation(info.ply)) info
      else info.dropVariation
    }

    def validateUsi(ply: Int, variation: List[String]): Validated[String, List[String]] =
      for {
        situation <-
          if (ply == replay.setup.startedAtTurn + 1) valid(replay.setup.situation)
          else replay moveAtPly ply map (_.fold(_.situationBefore, _.situationBefore)) toValid "No move found"
        usis <- variation.map(Usi.apply).sequence toValid "Invalid USI moves " + variation
        moves <-
          usis.foldLeft[Validated[String, (Situation, List[Either[Move, Drop]])]](valid(situation -> Nil)) {
            case (Validated.Valid((sit, moves)), usi: Usi.Move) =>
              sit.move(usi.orig, usi.dest, usi.promotion).leftMap(e => s"ply $ply $e") map { move =>
                move.situationAfter -> (Left(move) :: moves)
              }
            case (Validated.Valid((sit, moves)), usi: Usi.Drop) =>
              sit.drop(usi.role, usi.pos).leftMap(e => s"ply $ply $e") map { drop =>
                drop.situationAfter -> (Right(drop) :: moves)
              }
            case (failure, _) => failure
          }
      } yield moves._2.reverse map (_.fold(_.toUsi.usi, _.toUsi.usi))

    onlyMeaningfulVariations.foldLeft[WithErrors[List[Info]]]((Nil, Nil)) {
      case ((infos, errs), info) if info.variation.isEmpty => (info :: infos, errs)
      case ((infos, errs), info) =>
        validateUsi(info.ply, info.variation).fold(
          err => (info.dropVariation :: infos, LilaException(err) :: errs),
          usi => (info.copy(variation = usi) :: infos, errs)
        )
    } match {
      case (infos, errors) => analysis.copy(infos = infos.reverse) -> errors
    }
  }
}
