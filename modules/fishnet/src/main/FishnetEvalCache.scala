package lila.fishnet

import chess.format.Forsyth
import JsonApi.Request.Evaluation

final private class FishnetEvalCache(
    evalCacheApi: lila.evalCache.EvalCacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  val maxPlies = 15

  // indexes of positions to skip
  def skipPositions(game: Work.Game): Fu[List[Int]] =
    rawEvals(game).dmap(_.map(_._1))

  def evals(work: Work.Analysis): Fu[Map[Int, Evaluation]] =
    rawEvals(work.game) map {
      _.map { case (i, eval) =>
        val pv = eval.pvs.head
        i -> Evaluation(
          pv = pv.moves.value.toList,
          score = Evaluation
            .Score(
              cp = pv.score.cp,
              mate = pv.score.mate
            )
            .invertIf((i + work.startPly) % 2 == 1), // fishnet evals are from POV
          time = none,
          nodes = eval.knodes.intNodes.some,
          nps = none,
          depth = eval.depth.some
        )
      }.toMap
    }

  private def rawEvals(game: Work.Game): Fu[List[(Int, lila.evalCache.EvalCacheEntry.Eval)]] =
    chess.Replay
      .situationsFromUci(
        game.uciList.take(maxPlies - 1),
        game.initialFen,
        game.variant
      )
      .fold(
        _ => fuccess(Nil),
        _.zipWithIndex
          .map { case (sit, index) =>
            evalCacheApi.getSinglePvEval(game.variant, Forsyth >> sit) dmap2 { index -> _ }
          }
          .sequenceFu
          .map(_.flatten)
      )
}
