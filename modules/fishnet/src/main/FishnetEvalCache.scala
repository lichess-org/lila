package lila.fishnet

import chess.format.{ Forsyth, FEN }

import JsonApi.Request.Evaluation

private final class FishnetEvalCache(
    evalCacheApi: lila.evalCache.EvalCacheApi
) {

  val maxPlies = 15

  def plies(work: Work.Analysis): Fu[List[Int]] =
    rawEvals(work).map(_.map(_._1))

  def evals(work: Work.Analysis): Fu[Map[Int, Evaluation]] =
    rawEvals(work) map {
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

  private def rawEvals(work: Work.Analysis): Fu[List[(Int, lila.evalCache.EvalCacheEntry.Eval)]] =
    chess.Replay.situationsFromUci(
      work.game.uciList.take(maxPlies),
      work.game.initialFen,
      work.game.variant
    ).fold(
        _ => fuccess(Nil),
        _.zipWithIndex.map {
          case (game, index) =>
            evalCacheApi.getSinglePvEval(
              work.game.variant,
              FEN(Forsyth >> game)
            ) map2 { (eval: lila.evalCache.EvalCacheEntry.Eval) =>
                (index + work.startPly) -> eval
              }
        }.sequenceFu.map(_.flatten)
      )
}
