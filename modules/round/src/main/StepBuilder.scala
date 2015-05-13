package lila.round

import chess.format.Forsyth
import chess.format.pgn.Pgn
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
    initialFen: Option[String],
    possibleMoves: Boolean): JsArray = {
    chess.Replay.gameWhileValid(pgnMoves, initialFen, variant) match {
      case (games, error) =>
        error foreach logChessError(id)
        val lastPly = games.lastOption.??(_.turns)
        val steps = games.map { g =>
          val withDests = possibleMoves && !(lastPly == g.turns && g.situation.end)
          Step(
            ply = g.turns,
            move = for {
              pos <- g.board.history.lastMove
              san <- g.pgnMoves.lastOption
            } yield Step.Move(pos._1, pos._2, san),
            fen = Forsyth >> g,
            check = g.situation.check,
            dests = withDests ?? g.situation.destinations)
        }
        JsArray(a.fold[Seq[Step]](steps) {
          case (pgn, analysis) => applyAnalysisAdvices(
            id,
            applyAnalysisEvals(steps.toList, analysis),
            pgn, analysis, variant, possibleMoves)
        }.map(_.toJson))
    }
  }

  private def applyAnalysisEvals(steps: List[Step], analysis: Analysis): List[Step] =
    steps.zipWithIndex map {
      case (step, index) =>
        analysis.infos.lift(index - 1).fold(step) { info =>
          step.copy(
            eval = info.score map (_.ceiled.centipawns),
            mate = info.mate)
        }
    }

  private def applyAnalysisAdvices(
    gameId: String,
    steps: List[Step],
    pgn: Pgn,
    analysis: Analysis,
    variant: Variant,
    possibleMoves: Boolean): List[Step] =
    analysis.advices.foldLeft(steps) {
      case (steps, ad) => (for {
        before <- steps lift (ad.ply - 1)
        after <- steps lift ad.ply
      } yield steps.updated(ad.ply, after.copy(
        nag = ad.nag.symbol.some,
        comments = ad.makeComment(false, true) :: after.comments,
        variations = if (ad.info.variation.isEmpty) after.variations
        else makeVariation(gameId, before, ad.info, variant, possibleMoves).toList :: after.variations))
      ) | steps
    }

  private def makeVariation(gameId: String, fromStep: Step, info: Info, variant: Variant, possibleMoves: Boolean): List[Step] = {
    chess.Replay.gameWhileValid(info.variation take 20, fromStep.fen.some, variant) match {
      case (games, error) =>
        error foreach logChessError(gameId)
        val lastPly = games.lastOption.??(_.turns)
        games.drop(1).map { g =>
          val withDests = possibleMoves && !(lastPly == g.turns && g.situation.end)
          Step(
            ply = g.turns,
            move = for {
              pos <- g.board.history.lastMove
              (orig, dest) = pos
              san <- g.pgnMoves.lastOption
            } yield Step.Move(orig, dest, san),
            fen = Forsyth >> g,
            check = g.situation.check,
            dests = withDests ?? g.situation.destinations)
        }
    }
  }

  private val logChessError = (id: String) => (err: String) =>
    logwarn(s"Round API http://lichess.org/$id ${err.lines.toList.headOption}")
}
