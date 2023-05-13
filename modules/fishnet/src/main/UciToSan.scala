package lila.fishnet

import cats.data.Validated
import cats.data.Validated.valid
import cats.syntax.all.*
import chess.format.pgn.{ Dumper, SanStr }
import chess.format.Uci
import chess.{ Ply, MoveOrDrop, Replay, Situation }
import chess.MoveOrDrop.*

import lila.analyse.{ Analysis, Info }
import lila.base.LilaException

// Even though Info.variation is a List[SanStr]
// When we receive them from Fishnet clients it's actually a list of UCI moves.
// This converts them to San format. drops extra variations
private object UciToSan:

  type WithErrors[A] = (A, List[Exception])

  def apply(replay: Replay, analysis: Analysis): WithErrors[Analysis] =

    val pliesWithAdviceAndVariation: Set[Ply] = analysis.advices.view.collect {
      case a if a.info.hasVariation => a.ply
    }.toSet

    val onlyMeaningfulVariations: List[Info] = analysis.infos.map: info =>
      if pliesWithAdviceAndVariation(info.ply) then info
      else info.dropVariation

    def uciToPgn(ply: Ply, variation: List[String]): Validated[String, List[SanStr]] =
      for
        situation <-
          if ply == replay.setup.startedAtPly + 1 then valid(replay.setup.situation)
          else replay moveAtPly ply map (_.fold(_.situationBefore, _.situationBefore)) toValid "No move found"
        ucis <- variation.traverse(Uci.apply) toValid s"Invalid UCI moves $variation"
        moves <-
          ucis.foldLeft(valid[String, (Situation, List[MoveOrDrop])](situation -> Nil)) {
            case (Validated.Valid((sit, moves)), uci) => validateMove(moves, sit, ply, uci)
            case (failure, _)                         => failure
          }
      yield moves._2.reverse map (_.fold(Dumper.apply, Dumper.apply))

    def validateMove(acc: List[MoveOrDrop], sit: Situation, ply: Ply, md: Uci) =
      val result = md match
        case move: Uci.Move => sit.move(move.orig, move.dest, move.promotion)
        case drop: Uci.Drop => sit.drop(drop.role, drop.square)
      result.bimap(e => s"ply $ply $e", move => move.situationAfter -> (move :: acc))

    onlyMeaningfulVariations.foldLeft[WithErrors[List[Info]]]((Nil, Nil)) {
      case ((infos, errs), info) if info.variation.isEmpty => (info :: infos, errs)
      case ((infos, errs), info) =>
        uciToPgn(info.ply, SanStr raw info.variation).fold(
          err => (info.dropVariation :: infos, LilaException(err) :: errs),
          pgn => (info.copy(variation = pgn) :: infos, errs)
        )
    } match
      case (infos, errors) => analysis.copy(infos = infos.reverse) -> errors
