package lila.fishnet

import lila.tree.CloudEval
import JsonApi.Request.Evaluation

trait IFishnetEvalCache:
  def skipPositions(game: Work.Game): Fu[List[Int]]
  def evals(work: Work.Analysis): Fu[Map[Int, Evaluation]]

final private class FishnetEvalCache(getSinglePvEval: CloudEval.GetSinglePvEval)(using Executor)
    extends IFishnetEvalCache:

  val maxPlies = 15

  // indexes of positions to skip
  def skipPositions(game: Work.Game): Fu[List[Int]] =
    rawEvals(game).dmap(_._1F)

  def evals(work: Work.Analysis): Fu[Map[Int, Evaluation]] =
    rawEvals(work.game).map:
      _.map { (i, eval) =>
        val pv = eval.pvs.head
        i -> Evaluation(
          pv = pv.moves.value.toList,
          score = Evaluation
            .Score(
              cp = pv.score.cp,
              mate = pv.score.mate
            )
            .invertIf((work.startPly + i).isOdd), // fishnet evals are from POV
          time = none,
          nodes = eval.knodes.intNodes.some,
          nps = none,
          depth = eval.depth.some
        )
      }.toMap

  private def rawEvals(game: Work.Game): Fu[List[(Int, CloudEval)]] =
    chess
      .Position(game.variant, game.initialFen)
      .playPositions(game.uciList.take(maxPlies - 1))
      .fold(
        _ => fuccess(Nil),
        _.zipWithIndex
          .parallel: (sit, index) =>
            getSinglePvEval(sit).dmap2(index -> _)
          .map(_.flatten)
      )

object FishnetEvalCache:
  val mock: IFishnetEvalCache = new:
    def skipPositions(game: Work.Game): Fu[List[Int]] = fuccess(Nil)
    def evals(work: Work.Analysis): Fu[Map[Int, Evaluation]] = fuccess(Map.empty)
