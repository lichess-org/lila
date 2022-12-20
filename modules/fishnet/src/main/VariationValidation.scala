package lila.fishnet

import cats.data.Validated
import cats.data.Validated.valid
import cats.implicits._

import shogi.format.usi.{ UciToUsi, Usi }
import shogi.{ Game, Replay }

import lila.analyse.{ Analysis, Info }
import lila.base.LilaException

// validated usi strings and drops extra variations
private object VariationValidation {

  type WithErrors[A] = (A, List[Exception])

  def apply(game: Work.Game, analysis: Analysis): WithErrors[Analysis] = {

    lazy val games = Replay
      .gamesWhileValid(
        game.usiList,
        game.initialSfen,
        game.variant
      )
      ._1
      .toList

    lazy val init = games.head

    val pliesWithAdviceAndVariation: Set[Int] = analysis.advices.view.collect {
      case a if a.info.hasVariation => a.ply
    } to Set

    val onlyMeaningfulVariations: List[Info] = analysis.infos map { info =>
      if (pliesWithAdviceAndVariation(info.ply)) info
      else info.dropVariation
    }

    def validateUsi(ply: Int, variation: List[String]): Validated[String, List[Usi]] =
      for {
        game <- games lift (ply - init.startedAtPly - 1) toValid s"No game at $ply. ply"
        usis <- variation
          .map(s => Usi.apply(s).orElse(UciToUsi.apply(s)))
          .sequence toValid "Invalid USI moves " + variation
        validatedUsis <-
          usis.foldLeft[Validated[String, (Game, List[Usi])]](valid((game, Nil))) {
            case (Validated.Valid((game, usis)), usi) =>
              game(usi).leftMap(e => s"ply $ply $e") map { g =>
                (g, usi :: usis)
              }
            case (failure, _) => failure
          }
      } yield validatedUsis._2.reverse

    onlyMeaningfulVariations.foldLeft[WithErrors[List[Info]]]((Nil, Nil)) {
      case ((infos, errs), info) if info.variation.isEmpty => (info :: infos, errs)
      case ((infos, errs), info) =>
        validateUsi(info.ply, info.variation).fold(
          err => (info.dropVariation :: infos, LilaException(err) :: errs),
          usis => (info.copy(variation = usis.map(_.usi)) :: infos, errs)
        )
    } match {
      case (infos, errors) => analysis.copy(infos = infos.reverse) -> errors
    }
  }
}
