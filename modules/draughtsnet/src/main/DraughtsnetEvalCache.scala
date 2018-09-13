package lidraughts.draughtsnet

import draughts.format.{ Forsyth, FEN }

import JsonApi.Request.Evaluation

private final class DraughtsnetEvalCache(
    evalCacheApi: lidraughts.evalCache.EvalCacheApi
) {

  val maxPlies = 15

  // indexes of positions to skip
  def skipPositions(game: Work.Game): Fu[List[Int]] =
    rawEvals(game).map(_.map(_._1))

  def evals(work: Work.Analysis): Fu[Map[Int, Evaluation]] =
    rawEvals(work.game) map {
      _.map {
        case (i, eval) =>
          val pv = eval.pvs.head
          i -> Evaluation(
            pv = pv.moves.value.toList,
            score = Evaluation.Score(
              cp = pv.score.cp,
              mate = pv.score.mate
            ).invertIf((i + work.startPly) % 2 == 1), // draughtsnet evals are from POV
            time = none,
            nodes = eval.knodes.intNodes.some,
            nps = none,
            depth = eval.depth.some
          )
      }.toMap
    }

  private def rawEvals(game: Work.Game): Fu[List[(Int, lidraughts.evalCache.EvalCacheEntry.Eval)]] =
    draughts.Replay.situationsFromUci(
      game.uciList.take(maxPlies - 1),
      game.initialFen,
      game.variant
    ).fold(
        _ => fuccess(Nil),
        _.zipWithIndex.map {
          case (sit, index) =>
            evalCacheApi.getSinglePvEval(
              game.variant,
              FEN(Forsyth >> sit)
            ) map2 { (eval: lidraughts.evalCache.EvalCacheEntry.Eval) =>
                index -> eval
              }
        }.sequenceFu.map(_.flatten)
      )
}
