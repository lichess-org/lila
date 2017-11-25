package lila.fishnet

import chess.format.{ Forsyth, FEN }

import JsonApi.Request.Evaluation

private final class FishnetEvalCache(
    evalCacheApi: lila.evalCache.EvalCacheApi
) {

  val maxPlies = 12

  // indexes of positions to skip
  def skipPositions(game: Work.Game): Fu[List[Int]] =
    rawEvals(game).map(_.map(_._1))

  def evals(game: Work.Game): Fu[Map[Int, Evaluation]] =
    rawEvals(game) map {
      _.toMap.mapValues { eval =>
        val pv = eval.pvs.head
        Evaluation(
          pv = Some(pv.moves.value.toList mkString " "),
          score = Evaluation.Score(
            cp = pv.score.cp,
            mate = pv.score.mate
          ),
          time = none,
          nodes = eval.knodes.intNodes.some,
          nps = none,
          depth = eval.depth.some
        )
      }
    }

  private def rawEvals(game: Work.Game): Fu[List[(Int, lila.evalCache.EvalCacheEntry.Eval)]] =
    chess.Replay.situationsFromUci(
      game.uciList.take(maxPlies),
      game.initialFen,
      game.variant
    ).fold(
        _ => fuccess(Nil),
        _.zipWithIndex.map {
          case (sit, index) =>
            evalCacheApi.getSinglePvEval(
              game.variant,
              FEN(Forsyth >> sit)
            ) map2 { (eval: lila.evalCache.EvalCacheEntry.Eval) =>
                index -> eval
              }
        }.sequenceFu.map(_.flatten)
      )
}
