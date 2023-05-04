package lila.puzzle

import akka.pattern.ask
import Puzzle.{ BSONFields as F }
import ornicar.scalalib.ThreadLocalRandom.odds
import chess.format.{ BoardFen, Uci }

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*

final private[puzzle] class DailyPuzzle(
    colls: PuzzleColls,
    pathApi: PuzzlePathApi,
    renderer: lila.hub.actors.Renderer,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import BsonHandlers.given

  private val cache =
    cacheApi.unit[Option[DailyPuzzle.WithHtml]] {
      _.refreshAfterWrite(1 minutes)
        .buildAsyncFuture(_ => find)
    }

  def get: Fu[Option[DailyPuzzle.WithHtml]] = cache.getUnit

  private def find: Fu[Option[DailyPuzzle.WithHtml]] =
    (findCurrent orElse findNewBiased()) recover { case e: Exception =>
      logger.error("find daily", e)
      none
    } flatMapz makeDaily

  private def makeDaily(puzzle: Puzzle): Fu[Option[DailyPuzzle.WithHtml]] = {
    import makeTimeout.short
    renderer.actor ? DailyPuzzle.Render(puzzle, puzzle.fenAfterInitialMove.board, puzzle.line.head) map {
      case html: String => DailyPuzzle.WithHtml(puzzle, html).some
    }
  } recover { case e: Exception =>
    logger.warn("make daily", e)
    none
  }

  private def findCurrent =
    colls.puzzle {
      _.find($doc(F.day $gt nowInstant.minusDays(1)))
        .sort($sort desc F.day)
        .one[Puzzle]
    }

  private def findNewBiased(tries: Int = 0): Fu[Option[Puzzle]] =
    def tryAgainMaybe = (tries < 7) ?? findNewBiased(tries + 1)
    import PuzzleTheme.*
    findNew flatMap {
      case None => tryAgainMaybe
      case Some(p) if p.hasTheme(anastasiaMate, arabianMate) && !odds(3) =>
        tryAgainMaybe dmap (_ orElse p.some)
      case p => fuccess(p)
    }

  private def findNew: Fu[Option[Puzzle]] =
    colls
      .path {
        _.aggregateOne() { framework =>
          import framework.*
          val forbiddenThemes = List(PuzzleTheme.oneMove) :::
            odds(2).??(List(PuzzleTheme.checkFirst))
          Match(pathApi.select(PuzzleAngle.mix, PuzzleTier.top, 2150 to 2300)) -> List(
            Sample(3),
            Project($doc("ids" -> true, "_id" -> false)),
            UnwindField("ids"),
            PipelineOperator(
              $lookup.pipeline(
                from = colls.puzzle,
                as = "puzzle",
                local = "ids",
                foreign = "_id",
                pipe = List(
                  $doc(
                    "$match" -> $doc(
                      Puzzle.BSONFields.plays $gt 9000,
                      Puzzle.BSONFields.day $exists false,
                      Puzzle.BSONFields.issue $exists false,
                      Puzzle.BSONFields.themes $nin forbiddenThemes.map(_.key)
                    )
                  )
                )
              )
            ),
            UnwindField("puzzle"),
            ReplaceRootField("puzzle"),
            AddFields($doc("dayScore" -> $doc("$multiply" -> $arr("$plays", "$vote")))),
            Sort(Descending("dayScore")),
            Limit(1)
          )
        }
      }
      .flatMap { docOpt =>
        docOpt.flatMap(puzzleReader.readOpt) ?? { puzzle =>
          colls.puzzle {
            _.updateField($id(puzzle.id), F.day, nowInstant)
          } inject puzzle.some
        }
      }

object DailyPuzzle:
  type Try = () => Fu[Option[DailyPuzzle.WithHtml]]

  case class WithHtml(puzzle: Puzzle, html: String)

  case class Render(puzzle: Puzzle, fen: BoardFen, lastMove: Uci)
