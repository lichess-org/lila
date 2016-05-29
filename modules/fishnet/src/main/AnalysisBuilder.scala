package lila.fishnet

import org.joda.time.DateTime

import chess.format.Uci
import JsonApi.Request.{ CompleteAnalysis, PartialAnalysis, Evaluation }
import lila.analyse.{ Analysis, Info }
import lila.game.GameRepo

private object AnalysisBuilder {

  def apply(client: Client, work: Work.Analysis, evals: List[Evaluation]): Fu[Analysis] =
    partial(client, work, evals map some, isPartial = false)

  def partial(
    client: Client,
    work: Work.Analysis,
    evals: List[Option[Evaluation]],
    isPartial: Boolean = true): Fu[Analysis] = {

    val uciAnalysis = Analysis(
      id = work.game.id,
      infos = makeInfos(evals, work.game.moveList, work.startPly),
      startPly = work.startPly,
      uid = work.sender.userId,
      by = !client.lichess option client.userId.value,
      date = DateTime.now)

    GameRepo.game(uciAnalysis.id) flatMap {
      case None => fufail(AnalysisBuilder.GameIsGone(uciAnalysis.id))
      case Some(game) =>
        GameRepo.initialFen(game) flatMap { initialFen =>
          def debug = s"Analysis for ${game.id} by ${client.fullId}"
          chess.Replay(game.pgnMoves, initialFen, game.variant).fold(
            fufail(_),
            replay => UciToPgn(replay, uciAnalysis) match {
              case (analysis, errors) =>
                errors foreach { e => logger.warn(s"[UciToPgn] $debug $e") }
                if (analysis.valid) {
                  if (!isPartial && analysis.emptyRatio >= 1d / 10)
                    fufail(s"Analysis $debug has ${analysis.nbEmptyInfos} empty infos out of ${analysis.infos.size}")
                  else fuccess(analysis)
                }
                else fufail(s"[analysis] Analysis $debug is empty")
            })
        }
    }
  }

  private def makeInfos(evals: List[Option[Evaluation]], moves: List[String], startedAtPly: Int): List[Info] =
    (evals filterNot (_ ?? (_.isCheckmate)) sliding 2).toList.zip(moves).zipWithIndex map {
      case ((List(Some(before), Some(after)), move), index) => {
        val variation = before.cappedPvList match {
          case first :: rest if first != move => first :: rest
          case _                              => Nil
        }
        val best = variation.headOption flatMap Uci.Move.apply
        val info = Info(
          ply = index + 1 + startedAtPly,
          score = after.score.cp map lila.analyse.Score.apply,
          mate = after.score.mate,
          variation = variation,
          best = best)
        if (info.ply % 2 == 1) info.invert else info
      }
      case ((_, _), index) => Info(index + 1 + startedAtPly)
    }

  case class GameIsGone(id: String) extends lila.common.LilaException {
    val message = s"Analysis $id game is gone?!"
  }
}
