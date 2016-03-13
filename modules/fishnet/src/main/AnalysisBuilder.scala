package lila.fishnet

import org.joda.time.DateTime

import chess.format.Uci
import JsonApi.Request.{ PostAnalysis, AnalysisPly }
import lila.analyse.{ Analysis, Info }
import lila.game.GameRepo

object AnalysisBuilder {

  def apply(client: Client, work: Work.Analysis, data: PostAnalysis) = {
    val uciAnalysis = Analysis(
      id = work.game.id,
      infos = makeInfos(data.analysis, work.startPly),
      startPly = work.startPly,
      uid = work.sender.userId,
      by = !client.lichess option client.userId.value,
      date = DateTime.now)

    GameRepo.game(uciAnalysis.id) flatten s"Analysis ${uciAnalysis.id} game is gone?!" flatMap { game =>
      GameRepo.initialFen(game) flatMap { initialFen =>
        lazy val debug = s"Analysis ${game.id} from ${client.fullId}"
        chess.Replay(game.pgnMoves.pp, initialFen, game.variant).pp.fold(
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

  private def makeInfos(moves: List[AnalysisPly], startPly: Int) = moves.zipWithIndex map {
    case (move, index) =>
      val ply = startPly + index
      Info(
        ply = ply,
        score = move.score.cp map lila.analyse.Score.apply,
        mate = move.score.mate,
        variation = move.pv ?? (_.split(' ').take(Info.LineMaxPlies).toList),
        best = move.bestmove flatMap Uci.Move.apply)
  }
}
