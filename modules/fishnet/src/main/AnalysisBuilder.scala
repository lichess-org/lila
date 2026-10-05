package lila.fishnet

import chess.{ Position, Ply }
import chess.format.Uci
import chess.format.pgn.SanStr
import chess.eval.WhiteScore

import lila.tree.{ Analysis, Eval, Info }

import JsonApi.Request.Evaluation
import Evaluation.EvalOrSkip

final private class AnalysisBuilder(evalCache: IFishnetEvalCache)(using Executor):

  def apply(client: Client, work: Work.Analysis, evals: List[EvalOrSkip]): Fu[Analysis] =
    partial(client, work, evals.map(some), isPartial = false)

  def partial(
      client: Client,
      work: Work.Analysis,
      evals: List[Option[EvalOrSkip]],
      isPartial: Boolean = true
  ): Fu[Analysis] =
    evalCache.evals(work).flatMap { cachedFull =>
      /* remove first eval in partial analysis
       * to prevent the mobile app from thinking it's complete
       * https://github.com/lichess-org/lichobile/issues/722
       */
      val cached = if isPartial then cachedFull - 0 else cachedFull
      def debug = s"${work.game.variant.key} analysis for ${work.game.id} by ${client.fullId}"

      val setup = Position.AndFullMoveNumber(work.game.variant, work.game.initialFen)
      setup
        .playPositions(work.game.uciList)
        .fold(
          err => fufail(err.value),
          positions =>
            val (analysis, errors) = UciToSan(
              positions,
              setup.ply,
              Analysis(
                id = Analysis.Id(work.game.studyId, work.game.id),
                infos = makeInfos(mergeEvalsAndCached(work, evals, cached), work.game.uciList, work.startPly),
                startPly = work.startPly,
                fk = (!client.lichess).option(client.key.value),
                date = nowInstant,
                nodesPerMove = work.origin.map(_.nodesPerMove)
              )
            )
            errors.foreach(e => logger.debug(s"[UciToPgn] $debug $e"))
            if analysis.valid then
              if !isPartial && analysis.emptyRatio >= 1d / 10 then
                fufail:
                  s"${work.game.variant.key} analysis $debug has ${analysis.nbEmptyInfos} empty infos out of ${analysis.infos.size}"
              else fuccess(analysis)
            else fufail(s"${work.game.variant.key} analysis $debug is empty")
        )
    }

  private def mergeEvalsAndCached(
      work: Work.Analysis,
      evals: List[Option[EvalOrSkip]],
      cached: Map[Int, Evaluation]
  ): List[Option[Evaluation]] =
    evals.mapWithIndex:
      case (None, i) => cached.get(i)
      case (Some(EvalOrSkip.Evaluated(eval)), i) => cached.getOrElse(i, eval).some
      case (_, i) =>
        cached
          .get(i)
          .orElse:
            logger.error(s"Missing cached eval for skipped position at index $i in $work")
            none[Evaluation]

  private def makeInfos(
      evals: List[Option[Evaluation]],
      moves: List[Uci],
      startedAtPly: Ply
  ): List[Info] =
    val ambiguousEnd = evals.lastOption.flatten.exists(_.score.isEmpty)
    (if ambiguousEnd then evals.init else evals)
      .sliding(2)
      .toList
      .zip(moves)
      .mapWithIndex:
        case ((List(Some(before), Some(after)), move), index) =>
          val variation = before.cappedPv match
            case first :: rest if first != move => first :: rest
            case _ => Nil
          val best = variation.headOption
          val ply = startedAtPly + index + 1
          Info(
            ply = ply,
            eval = Eval(after.score.map(WhiteScore(_, ply.turn)), best),
            variation = variation.map(uci => SanStr(uci.uci)) // temporary, for UciToSan
          )
        case ((_, _), index) => Info(startedAtPly + index + 1, lila.tree.evals.empty, Nil)
