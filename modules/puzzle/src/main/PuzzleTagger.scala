package lila.puzzle

import chess.{ Divider, Division }
import reactivemongo.akkastream.cursorProducer

import lila.common.LilaStream
import lila.db.dsl.{ *, given }

final private class PuzzleTagger(colls: PuzzleColls, openingApi: PuzzleOpeningApi)(using
    ec: Executor,
    mat: akka.stream.Materializer
):
  import BsonHandlers.given

  private[puzzle] def addAllMissing: Funit =
    colls.puzzle:
      _.find($doc(Puzzle.BSONFields.tagMe -> true))
        .cursor[Puzzle]()
        .documentSource()
        .throttle(500, 1.second)
        .mapAsyncUnordered(2)(p => openingApi.updateOpening(p).inject(p))
        .mapAsyncUnordered(2)(p => addPhase(p).inject(p))
        .mapAsyncUnordered(2)(checkFirstTheme)
        .runWith(LilaStream.sinkCount)
        .chronometer
        .log(logger)(count => s"Done tagging $count puzzles")
        .result
        .void

  private def addPhase(puzzle: Puzzle): Funit =
    puzzle.boardAfterInitialMove match
      case Some(position) =>
        val theme = Divider(List(position.board)) match
          case Division(None, Some(_), _) => PuzzleTheme.endgame
          case Division(Some(_), None, _) => PuzzleTheme.middlegame
          case _ => PuzzleTheme.opening
        colls.puzzle:
          _.update
            .one(
              $id(puzzle.id),
              $addToSet(Puzzle.BSONFields.themes -> theme.key) ++ $unset(Puzzle.BSONFields.tagMe)
            )
            .void
      case None =>
        logger.error(s"Can't compute phase of puzzle $puzzle")
        funit

  private def checkFirstTheme(puzzle: Puzzle): Funit = {
    for
      init <- puzzle.boardAfterInitialMove
      if !puzzle.hasTheme(PuzzleTheme.mateIn1)
      move <- puzzle.line.tail.headOption
      first <- init.move(move).toOption.map(_.after)
    yield first.check
  }.exists(_.yes)
    .so:
      colls
        .round:
          _.update
            .one(
              $id(PuzzleRound.Id(UserId.lichess, puzzle.id).toString),
              $addToSet(PuzzleRound.BSONFields.themes -> PuzzleRound.Theme(PuzzleTheme.checkFirst.key, true))
            )
        .zip(colls.puzzle {
          _.update.one(
            $id(puzzle.id),
            $addToSet(Puzzle.BSONFields.themes -> PuzzleTheme.checkFirst.key)
          )
        })
        .void
