package lila.puzzle
import chess.format.{ BoardFen, Uci }
import scalalib.ThreadLocalRandom.odds

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*

import Puzzle.BSONFields as F

final private[puzzle] class DailyPuzzle(
    colls: PuzzleColls,
    pathApi: PuzzlePathApi,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  import BsonHandlers.given

  private val cache =
    cacheApi.unit[Option[DailyPuzzle.WithHtml]]:
      _.refreshAfterWrite(1.minutes).buildAsyncFuture(_ => find)

  def get: Fu[Option[DailyPuzzle.WithHtml]] = cache.getUnit

  private def find: Fu[Option[DailyPuzzle.WithHtml]] =
    (findCurrent
      .orElse(findNewBiased()))
      .recover { case e: Exception =>
        logger.error("find daily", e)
        none
      }
      .flatMapz(makeDaily)

  private def makeDaily(puzzle: Puzzle): Fu[Option[DailyPuzzle.WithHtml]] = {
    lila.common.Bus
      .ask[Html, DailyPuzzle.Render]:
        DailyPuzzle.Render(puzzle, puzzle.fenAfterInitialMove.board, puzzle.line.head, _)
      .map: html =>
        DailyPuzzle.WithHtml(puzzle, html).some
  }.recover { case e: Exception =>
    logger.warn("make daily", e)
    none
  }

  private def findCurrent = colls.puzzle:
    _.find($doc(F.day.$gt(nowInstant.minusDays(1))))
      .sort($sort.desc(F.day))
      .one[Puzzle]

  private val maxTries = 10
  private val minPlaysBase = 9000
  private def findNewBiased(tries: Int = 0): Fu[Option[Puzzle]] =
    def tryAgainMaybe = (tries < maxTries).so(findNewBiased(tries + 1))
    import PuzzleTheme.*
    val minPlays = minPlaysBase * (maxTries - tries) / maxTries
    findNew(minPlays).flatMap:
      case None => tryAgainMaybe
      case Some(p) if p.hasTheme(anastasiaMate, arabianMate) && !odds(3) =>
        tryAgainMaybe.dmap(_.orElse(p.some))
      case p => fuccess(p)

  private def findNew(minPlays: Int): Fu[Option[Puzzle]] =
    colls
      .path:
        _.aggregateOne(): framework =>
          import framework.*
          val forbiddenThemes = List(PuzzleTheme.oneMove) :::
            odds(2).so(List(PuzzleTheme.checkFirst))
          Match(pathApi.select(PuzzleAngle.mix, PuzzleTier.top, 2150 to 2300)) -> List(
            Sample(3),
            Project($doc("ids" -> true, "_id" -> false)),
            UnwindField("ids"),
            PipelineOperator:
              $lookup.pipeline(
                from = colls.puzzle,
                as = "puzzle",
                local = "ids",
                foreign = "_id",
                pipe = List(
                  $doc(
                    "$match" -> $doc(
                      Puzzle.BSONFields.plays.$gt(minPlays),
                      Puzzle.BSONFields.day.$exists(false),
                      Puzzle.BSONFields.issue.$exists(false),
                      Puzzle.BSONFields.themes.$nin(forbiddenThemes.map(_.key))
                    )
                  )
                )
              )
            ,
            UnwindField("puzzle"),
            ReplaceRootField("puzzle"),
            AddFields($doc("dayScore" -> $doc("$multiply" -> $arr("$plays", "$vote")))),
            Sort(Descending("dayScore")),
            Limit(1)
          )
      .flatMap:
        _.flatMap(puzzleReader.readOpt).so { puzzle =>
          colls.puzzle(_.updateField($id(puzzle.id), F.day, nowInstant)).inject(puzzle.some)
        }

object DailyPuzzle:
  type Try = () => Fu[Option[DailyPuzzle.WithHtml]]

  case class WithHtml(puzzle: Puzzle, html: Html)

  case class Render(puzzle: Puzzle, fen: BoardFen, lastMove: Uci, promise: Promise[Html])
