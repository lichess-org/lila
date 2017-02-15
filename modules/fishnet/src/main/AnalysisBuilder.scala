package lila.fishnet

import org.joda.time.DateTime

import chess.format.Uci
import JsonApi.Request.Evaluation
import lila.analyse.{ Analysis, Info }
import lila.game.GameRepo
import lila.tree.Eval

private object AnalysisBuilder {

  def complete(
    client: Client,
    work: Work.Analysis,
    evals: List[Evaluation]): Fu[Analysis] = {

    val infos = evals.filterNot(_.score.isCheckmate).sliding(2).toList
      .zip(work.game.moveList).zipWithIndex map {
        case ((List(before, after), move), index) => {
          val variation = before.cappedPvList match {
            case first :: rest if first != move => first :: rest
            case _                              => Nil
          }
          val best = variation.headOption flatMap Uci.Move.apply
          val ply = index + 1 + work.startPly
          Info(
            ply = ply,
            eval = Eval(after.score, best).invertIf(ply % 2 == 1),
            variation = variation)
        }
      }

    val uciAnalysis = Analysis(
      id = work.game.id,
      infos = infos,
      startPly = work.startPly,
      uid = work.sender.userId,
      by = !client.lichess option client.userId.value,
      date = DateTime.now
    )

    GameRepo.game(uciAnalysis.id) flatMap {
      case None => fufail(AnalysisBuilder.GameIsGone(uciAnalysis.id))
      case Some(game) =>
        GameRepo.initialFen(game) flatMap { initialFen =>
          def debug = s"${game.variant.key} analysis for ${game.id} by ${client.fullId}"
          chess.Replay(game.pgnMoves, initialFen, game.variant).fold(
            fufail(_),
            replay => UciToPgn(replay, uciAnalysis.infos, uciAnalysis.advices) match {
              case (infos, errors) =>
                errors foreach { e => logger.debug(s"[UciToPgn] $debug $e") }
                val analysis = uciAnalysis.copy(infos = infos)
                if (analysis.valid) {
                  if (analysis.emptyRatio >= 1d / 10)
                    fufail(s"${game.variant.key} analysis $debug has ${analysis.nbEmptyInfos} empty infos out of ${analysis.infos.size}")
                  else fuccess(analysis)
                }
                else fufail(s"${game.variant.key} analysis $debug is empty")
            }
          )
        }
    }
  }

  def partial(
    client: Client,
    work: Work.Analysis,
    evals: List[Option[Evaluation]]): Fu[Analysis.Partial] = {

    val infos = evals.filterNot(_ ?? (_.score.isCheckmate)).sliding(2).toList
      .zip(work.game.moveList).zipWithIndex map {
      case ((List(Some(before), Some(after)), move), index) => {
        val variation = before.cappedPvList match {
          case first :: rest if first != move => first :: rest
          case _ => Nil
        }
        val best = variation.headOption flatMap Uci.Move.apply
          val ply = index + 1 + work.startPly
          Info(
            ply = ply,
            eval = Eval(after.score, best).invertIf(ply % 2 == 1),
          variation = variation
          ).some
        }
        case ((_, _), index) => none
      }

    val uciAnalysis = Analysis.Partial(
      id = work.game.id,
      infos = infos,
      startPly = work.startPly)

    GameRepo.game(uciAnalysis.id) flatMap {
      case None => fufail(AnalysisBuilder.GameIsGone(uciAnalysis.id))
      case Some(game) =>
        GameRepo.initialFen(game) flatMap { initialFen =>
          def debug = s"${game.variant.key} analysis for ${game.id} by ${client.fullId}"
          chess.Replay(game.pgnMoves, initialFen, game.variant).fold(
            fufail(_),
            replay => UciToPgn(replay, uciAnalysis.infos, uciAnalysis.advices) match {
              case (infos, errors) =>
                errors foreach { e => logger.debug(s"[UciToPgn] $debug $e") }
                val analysis = uciAnalysis.copy(infos = infos)
                if (analysis.valid) fuccess(analysis)
                else fufail(s"${game.variant.key} analysis $debug is empty")
            })
        }
    }
  }

  case class GameIsGone(id: String) extends lila.common.LilaException {
    val message = s"Analysis $id game is gone?!"
  }
}
