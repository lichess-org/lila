package lila.fishnet

import chess.format.Uci
import chess.format.pgn.SanStr
import chess.{ Position, Ply }

import lila.analyse.{ Analysis, Info }
import lila.core.lilaism.LilaException

// Even though Info.variation is a List[SanStr]
// When we receive them from Fishnet clients it's actually a list of UCI moves.
// This converts them to San format. drops extra variations
private object UciToSan:

  type WithErrors[A] = (A, List[Exception])

  def apply(positions: List[Position], startedAt: Ply, analysis: Analysis): WithErrors[Analysis] =

    val pliesWithAdviceAndVariation: Set[Ply] = analysis.advices.view.collect {
      case a if a.info.hasVariation => a.ply
    }.toSet

    val onlyMeaningfulVariations: List[Info] = analysis.infos.map: info =>
      if pliesWithAdviceAndVariation(info.ply) then info
      else info.dropVariation

    def uciToSan(ply: Ply, variation: List[String]): Either[String, List[SanStr]] =
      for
        position <- positions.lift(ply.value - startedAt.value - 1).toRight(s"No move found at ply $ply")
        moves <- position.play(variation, ply)(_.move.toSanStr).leftMap(_.value)
      yield moves

    onlyMeaningfulVariations.foldLeft[WithErrors[List[Info]]]((Nil, Nil)):
      case ((infos, errs), info) if info.variation.isEmpty => (info :: infos, errs)
      case ((infos, errs), info) =>
        uciToSan(info.ply, SanStr.raw(info.variation)).fold(
          err => (info.dropVariation :: infos, LilaException(err) :: errs),
          sans => (info.copy(variation = sans) :: infos, errs)
        )
    match
      case (infos, errors) => analysis.copy(infos = infos.reverse) -> errors
