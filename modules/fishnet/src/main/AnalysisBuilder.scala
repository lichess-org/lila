package lila.fishnet

import org.joda.time.DateTime

import chess.format.Uci
import JsonApi.Request.{ PostAnalysis, Evaluation }
import lila.analyse.{ Analysis, Info }
import lila.game.GameRepo

private object AnalysisBuilder {

  def apply(client: Client, work: Work.Analysis, data: PostAnalysis) = {

    val uciAnalysis = Analysis(
      id = work.game.id,
      infos = makeInfos(data.analysis, work.game.moveList, work.startPly),
      startPly = work.startPly,
      uid = work.sender.userId,
      by = !client.lichess option client.userId.value,
      date = DateTime.now)

    GameRepo.game(uciAnalysis.id) flatMap {
      case None => fufail(AnalysisBuilder.GameIsGone(uciAnalysis.id))
      case Some(game) =>
        GameRepo.initialFen(game) flatMap { initialFen =>
          def debug = s"Analysis ${game.id} from ${client.fullId}"
          chess.Replay(game.pgnMoves, initialFen, game.variant).fold(
            fufail(_),
            replay => UciToPgn(replay, uciAnalysis) match {
              case (analysis, errors) =>
                errors foreach { e => log.warn(s"[UciToPgn] $debug $e") }
                if (analysis.valid) {
                  if (analysis.emptyRatio >= 1d / 10)
                    fufail(s"Analysis $debug has ${analysis.nbEmptyInfos} empty infos out of ${analysis.infos.size}")
                  else fuccess(analysis)
                }
                else fufail(s"[analysis] Analysis $debug is empty")
            })
        }
    }
  }

  private def makeInfos(evals: List[Evaluation], moves: List[String], startedAtPly: Int): List[Info] =
    (evals filterNot (_.isCheckmate) sliding 2).toList.zip(moves).zipWithIndex map {
      case ((List(before, after), move), index) => {
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
    }

  case class GameIsGone(id: String) extends lila.common.LilaException {
    val message = s"Analysis $id game is gone?!"
  }
}
