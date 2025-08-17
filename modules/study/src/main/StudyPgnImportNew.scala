package lila.study

import chess.format.pgn.{ Glyphs, ParsedPgnTree, PgnNodeData, PgnStr, Tags, Tag }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.{ ErrorStr, Node as PgnNode, ByColor }
import monocle.syntax.all.*

import lila.core.LightUser
import lila.tree.Node.{ Comment, Comments }
import lila.tree.{ ImportResult, Metas, NewBranch, NewRoot, Clock, ParseImport }

// This code is still unused
object StudyPgnImportNew:

  import StudyPgnImport.Context

  case class Result(
      root: NewRoot,
      variant: chess.variant.Variant,
      tags: Tags,
      end: Option[StudyPgnImport.Ending]
  )

  def apply(pgn: PgnStr, contributors: List[LightUser]): Either[ErrorStr, Result] =
    ParseImport.full(pgn).map { case ImportResult(game, result, replay, initialFen, parsedPgn, _) =>
      val annotator = StudyPgnImport.findAnnotator(parsedPgn, contributors)
      StudyPgnImport.parseComments(parsedPgn.initialPosition.comments, annotator) match
        case (shapes, _, _, comments) =>
          val tc = parsedPgn.tags.timeControl
          val clock = tc.map(_.limit).map(Clock(_, true.some))
          val setup = Context(replay.setup.position, ByColor.fill(clock), tc, replay.setup.ply)
          val root: NewRoot =
            NewRoot(
              Metas(
                ply = replay.setup.ply,
                fen = initialFen | replay.setup.position.variant.initialFen,
                check = replay.setup.position.check,
                dests = None,
                drops = None,
                eval = None,
                shapes = shapes,
                comments = comments,
                gamebook = None,
                glyphs = Glyphs.empty,
                opening = None,
                crazyData = replay.setup.position.crazyData,
                clock = clock
              ),
              parsedPgn.tree.flatMap(makeTree(setup, _, annotator))
            )

          val gameEnd = result.map: res =>
            StudyPgnImport.Ending(
              status = res.status,
              points = res.points,
              resultText = chess.Outcome.showPoints(res.points.some),
              statusText = lila.tree.StatusText(res.status, res.winner, replay.state.position.variant)
            )

          val commented =
            if root.tree.map(_.lastMainlineNode).exists(_.value.metas.comments.value.nonEmpty) then root
            else
              gameEnd.map(StudyPgnImport.endComment).fold(root) { comment =>
                root
                  .focus(_.tree.some)
                  .modify(_.modifyLastMainlineNode(_.focus(_.value.metas.comments).modify(_ + comment)))
              }
          Result(
            root = commented,
            variant = game.position.variant,
            tags = PgnTags
              .withRelevantTags(parsedPgn.tags, Set(Tag.WhiteClock, Tag.BlackClock)),
            end = gameEnd
          )
    }

  private def makeTree(
      context: Context,
      node: ParsedPgnTree,
      annotator: Option[Comment.Author]
  ): Option[PgnNode[NewBranch]] =
    node.mapAccumlOption_(context): (context, data) =>
      transform(context, data, annotator)

  private def transform(
      context: Context,
      data: PgnNodeData,
      annotator: Option[Comment.Author]
  ): (Context, Option[NewBranch]) =
    data
      .san(context.currentPosition)
      .map(moveOrDrop =>
        val game = moveOrDrop.after
        val currentPly = context.ply.next
        val uci = moveOrDrop.toUci
        val id = UciCharPair(uci)
        val sanStr = moveOrDrop.toSanStr
        val (shapes, clock, emt, comments) = StudyPgnImport.parseComments(data.metas.comments, annotator)
        val mover = !game.color
        val computedClock: Option[Clock] = clock
          .map(Clock(_, trust = true.some))
          .orElse:
            (context.clocks(mover), emt)
              .mapN(StudyPgnImport.guessNewClockState(_, context.timeControl, _))
          .filter(_.positive)
        val newBranch =
          NewBranch(
            id = id,
            move = Uci.WithSan(uci, sanStr),
            comp = false,
            forceVariation = false,
            Metas(
              ply = currentPly,
              fen = Fen.write(game, currentPly.fullMoveNumber),
              check = game.check,
              dests = None,
              drops = None,
              eval = None,
              shapes = shapes,
              comments = comments,
              gamebook = None,
              glyphs = data.metas.glyphs,
              opening = None,
              clock = computedClock,
              crazyData = game.crazyData
            )
          )

        (Context(game, context.clocks, context.timeControl, currentPly), newBranch.some)
      )
      .toOption
      .match
        case Some(branch) => branch
        case None => (context, None)
