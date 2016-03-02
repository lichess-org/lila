package lila.round

import chess.format.{ Forsyth, Uci, UciCharPair }
import chess.format.pgn.Pgn
import chess.opening._
import chess.variant.Variant
import lila.analyse.{ Analysis, Info }
import lila.socket.Step

import play.api.libs.json._

object StepBuilder {

  def apply(
    id: String,
    pgnMoves: List[String],
    variant: Variant,
    a: Option[(Pgn, Analysis)],
    initialFen: String,
    withOpening: Boolean): JsArray = {
    chess.Replay.gameWhileValid(pgnMoves, initialFen, variant) match {
      case (games, error) =>
        error foreach logChessError(id)
        val lastPly = games.lastOption.??(_.turns)
        val openingOf: String => Option[FullOpening] =
          if (withOpening && Variant.openingSensibleVariants(variant)) FullOpeningDB.findByFen
          else _ => None
        val steps = games.map { g =>
          val fen = Forsyth >> g
          Step(
            id = g.board.history.lastMove.map(UciCharPair.apply),
            ply = g.turns,
            move = for {
              uci <- g.board.history.lastMove
              san <- g.pgnMoves.lastOption
            } yield Step.Move(uci, san),
            fen = fen,
            check = g.situation.check,
            dests = None,
            opening = openingOf(fen),
            drops = None,
            crazyData = g.situation.board.crazyData)
        }
        JsArray(a.fold[Seq[Step]](steps) {
          case (pgn, analysis) => applyAnalysisAdvices(
            id,
            applyAnalysisEvals(steps, analysis),
            pgn, analysis, variant)
        }.map(_.toJson))
    }
  }

  private def applyAnalysisEvals(steps: List[Step], analysis: Analysis): List[Step] =
    steps.zipWithIndex map {
      case (step, index) =>
        analysis.infos.lift(index - 1).fold(step) { info =>
          step.copy(
            eval = Step.Eval(
              cp = info.score.map(_.ceiled.centipawns),
              mate = info.mate,
              best = info.best).some)
        }
    }

  private def applyAnalysisAdvices(
    gameId: String,
    steps: List[Step],
    pgn: Pgn,
    analysis: Analysis,
    variant: Variant): List[Step] =
    analysis.advices.foldLeft(steps) {
      case (steps, ad) =>
        val index = ad.ply - analysis.startPly
        (for {
          before <- steps lift (index - 1)
          after <- steps lift index
        } yield steps.updated(index, after.copy(
          nag = ad.nag.symbol.some,
          comments = ad.makeComment(false, true) :: after.comments,
          variations = if (ad.info.variation.isEmpty) after.variations
          else makeVariation(gameId, before, ad.info, variant).toList :: after.variations))
        ) | steps
    }

  private def makeVariation(gameId: String, fromStep: Step, info: Info, variant: Variant): List[Step] = {
    chess.Replay.gameWhileValid(info.variation take 20, fromStep.fen, variant) match {
      case (games, error) =>
        error foreach logChessError(gameId)
        val lastPly = games.lastOption.??(_.turns)
        games.drop(1).map { g =>
          Step(
            id = g.board.history.lastMove.map(UciCharPair.apply),
            ply = g.turns,
            move = for {
              uci <- g.board.history.lastMove
              san <- g.pgnMoves.lastOption
            } yield Step.Move(uci, san),
            fen = Forsyth >> g,
            check = g.situation.check,
            dests = None,
            drops = None,
            crazyData = g.situation.board.crazyData)
        }
    }
  }

  private val logChessError = (id: String) => (err: String) =>
    logwarn(s"Round API http://lichess.org/$id ${err.lines.toList.headOption}")
}
