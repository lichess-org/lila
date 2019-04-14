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
        case (i, eval, sit) =>
          val pv = eval.pvs.head
          val ucis = draughts.Replay.ucis(pv.moves.value.toList, sit, true).fold(
            err => {
              logger.warn(s"DraughtsnetEvalCache: Could not parse ${work.game.variant.key} pv: ${pv.moves.value.toList.mkString(" ")}")
              Nil
            },
            suc => suc
          )
          i -> Evaluation(
            pv = ucis,
            score = Evaluation.Score(
              cp = pv.score.cp,
              win = pv.score.win
            ).invertIf((i + work.startPly) % 2 == 1), // draughtsnet evals are from POV
            time = none,
            nodes = eval.knodes.intNodes.some,
            nps = none,
            depth = eval.depth.some
          )
      }.toMap
    }

  private def rawEvals(game: Work.Game): Fu[List[(Int, lidraughts.evalCache.EvalCacheEntry.Eval, draughts.Situation)]] =
    draughts.Replay.situationsFromUci(
      game.uciList.take(maxPlies - 1),
      game.initialFen,
      game.variant,
      finalSquare = true
    ).fold(
        _ => fuccess(Nil),
        _.zipWithIndex.map {
          case (sit, index) =>
            evalCacheApi.getSinglePvEval(
              game.variant,
              FEN(Forsyth >> sit)
            ) map2 { (eval: lidraughts.evalCache.EvalCacheEntry.Eval) =>
                (index, eval, sit)
              }
        }.sequenceFu.map(_.flatten)
      )
}
