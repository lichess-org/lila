package lila.fishnet

import chess.format.{ Forsyth, FEN }

private final class FishnetEvalCache(
    evalCacheApi: lila.evalCache.EvalCacheApi
) {

  def plies(work: Work.Analysis): Fu[List[Int]] =
    evals(work).map(_.keys.toList)

  def evals(work: Work.Analysis): Fu[FishnetEvalCache.CachedEvals] =
    chess.Replay.situationsFromUci(
      work.game.uciList.take(12),
      work.game.initialFen,
      work.game.variant
    ).fold(
        _ => fuccess(Map.empty),
        _.zipWithIndex.map {
          case (game, index) =>
            evalCacheApi.getSinglePvEval(
              work.game.variant,
              FEN(Forsyth >> game)
            ) map2 { (eval: lila.evalCache.EvalCacheEntry.Eval) =>
                (index + work.startPly) -> eval
              }
        }.sequenceFu.map(_.flatten.toMap)
      )
}

private final object FishnetEvalCache {

  type CachedEvals = Map[Int, lila.evalCache.EvalCacheEntry.Eval]
}
