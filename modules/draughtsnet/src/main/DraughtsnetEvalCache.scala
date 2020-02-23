package lidraughts.draughtsnet

import JsonApi.Request.Evaluation
import draughts.DraughtsGame
import draughts.format.{ FEN, Forsyth }
import lidraughts.evalCache.EvalCacheEntry._
import lidraughts.tree.Eval.Score
import scalaz.NonEmptyList

private final class DraughtsnetEvalCache(
    evalCacheApi: lidraughts.evalCache.EvalCacheApi
) {

  val maxPlies = 15

  def putEval(work: Work.Commentary, result: JsonApi.Request.Evaluation) = {
    val game = DraughtsGame(work.game.variant.some, work.currentFen.value.some)
    UciToPdn(game.situation, result.pv).fold(
      err => none,
      moveList => {
        val scorePov = result.score.invertIf(game.situation.color.black) // draughtsnet evals are from POV
        val score = Score(Either.cond(scorePov.win.nonEmpty, scorePov.win.get, scorePov.cp.get))
        val candidate = for {
          moves <- moveList.toNel
          pv <- List(Pv(score, Moves(moves))).toNel
          user <- work.acquired.map(_.userId.value)
          depth <- result.depth
        } yield Input.Candidate(
          variant = work.game.variant,
          fen = work.currentFen.value,
          eval = Eval(
            pvs = pv,
            knodes = Knodes(result.nodes.getOrElse(1)),
            depth = depth,
            by = user,
            trust = Trust(1)
          )
        )
        candidate.foreach { c =>
          evalCacheApi.put(c.eval.by, c, none)
        }
        candidate.map(_.eval)
      }
    )
  }

  // indexes of positions to skip
  def skipPositions(game: Work.Game, minNodes: Int = 0): Fu[List[Int]] =
    rawEvals(game, minNodes).map(_.map(_._1))

  def evals(work: Work.Analysis, minNodes: Int = 0): Fu[Map[Int, Evaluation]] =
    rawEvals(work.game, minNodes) map {
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

  private def rawEvals(game: Work.Game, minNodes: Int = 0): Fu[List[(Int, lidraughts.evalCache.EvalCacheEntry.Eval, draughts.Situation)]] =
    draughts.Replay.situationsFromUci(
      // check whole game for simuls and studies as any move could be in evalcache (often with higher quality / depth)
      game.uciList.take(if (game.isSimul || game.isStudy) Env.current.analyser.maxPlies else maxPlies - 1),
      game.initialFen,
      game.variant,
      finalSquare = true
    ).fold(
        _ => fuccess(Nil),
        _.zipWithIndex.map {
          case (sit, index) =>
            evalCacheApi.getSinglePvEval(
              game.variant,
              FEN(Forsyth >> sit),
              minNodes
            ) map2 { (eval: lidraughts.evalCache.EvalCacheEntry.Eval) =>
                (index, eval, sit)
              }
        }.sequenceFu.map(_.flatten)
      )
}
