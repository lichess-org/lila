package lila.fishnet

import org.joda.time.DateTime

import chess.format.Uci
import lila.analyse.{ Analysis, Info }
import JsonApi.Request.{ PostAnalysis, AnalysisPly }

object ToAnalysis {

  def apply(client: Client, work: Work.Analysis, data: PostAnalysis): Option[Analysis] = {
    Analysis(
      id = work.game.id,
      infos = makeInfos(data.analysis, work.startPly),
      startPly = work.startPly,
      uid = work.sender.userId,
      by = !client.lichess option client.userId.value,
      date = DateTime.now
    ).some
  }

  private def makeInfos(moves: List[AnalysisPly], startPly: Int) = moves.zipWithIndex map {
    case (move, index) =>
      val ply = startPly + index
      Info(
        ply = ply,
        score = move.score.cp map lila.analyse.Score.apply,
        mate = move.score.mate,
        variation = move.pv ?? (_.split(' ').toList),
        best = move.bestmove flatMap Uci.Move.apply)
  }
}
