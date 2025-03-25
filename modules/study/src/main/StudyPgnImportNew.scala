package lila.study

import chess.MoveOrDrop.*
import chess.format.pgn.{ Glyphs, ParsedPgn, ParsedPgnTree, PgnNodeData, PgnStr, Tags, Tag }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.{ ErrorStr, Node as PgnNode, ByColor }
import monocle.syntax.all.*

import lila.core.LightUser
import lila.tree.Node.{ Comment, Comments }
import lila.tree.{ ImportResult, Metas, NewBranch, NewRoot, NewTree, Clock }

/** This code is still unused, and is now out of sync with the StudyPgnImport it's supposed to replace. Some
  * features are missing that are present in StudyPgnImport, such as the ability to replay clock states.
  */
object StudyPgnImportNew:

  import StudyPgnImport.Context

  case class Result(
      root: NewRoot,
      variant: chess.variant.Variant,
      tags: Tags,
      end: Option[StudyPgnImport.Ending]
  )

  def apply(pgn: PgnStr, contributors: List[LightUser]): Either[ErrorStr, Result] =
    lila.tree.parseImport(pgn).map { case ImportResult(game, result, replay, initialFen, parsedPgn) =>
      val annotator = StudyPgnImport.findAnnotator(parsedPgn, contributors)
      StudyPgnImport.parseComments(parsedPgn.initialPosition.comments, annotator) match
        case (shapes, _, _, comments) =>
          val tc    = parsedPgn.tags.timeControl
          val clock = tc.map(_.limit).map(Clock(_, true.some))
          val setup = Context(replay.setup, ByColor.fill(clock), tc)
          val root: NewRoot =
            NewRoot(
              Metas(
                ply = replay.setup.ply,
                fen = initialFen | game.board.variant.initialFen,
                check = replay.setup.situation.check,
                dests = None,
                drops = None,
                eval = None,
                shapes = shapes,
                comments = comments,
                gamebook = None,
                glyphs = Glyphs.empty,
                opening = None,
                crazyData = replay.setup.situation.board.crazyData,
                clock = clock
              ),
              parsedPgn.tree.flatMap(makeTree(setup, _, annotator))
            )

          val gameEnd = result.map: res =>
            StudyPgnImport.Ending(
              status = res.status,
              points = res.points,
              resultText = chess.Outcome.showPoints(res.points.some),
              statusText = lila.tree.StatusText(res.status, res.winner, game.board.variant)
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
            variant = game.board.variant,
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
      .san(context.currentGame.situation)
      .map(moveOrDrop =>
        val game   = moveOrDrop.applyGame(context.currentGame)
        val uci    = moveOrDrop.toUci
        val id     = UciCharPair(uci)
        val sanStr = moveOrDrop.toSanStr
        val newBranch = StudyPgnImport.parseComments(data.metas.comments, annotator) match
          case (shapes, clock, emt, comments) =>
            NewBranch(
              id = id,
              move = Uci.WithSan(uci, sanStr),
              comp = false,
              forceVariation = false,
              Metas(
                ply = game.ply,
                fen = Fen.write(game),
                check = game.situation.check,
                dests = None,
                drops = None,
                eval = None,
                shapes = shapes,
                comments = comments,
                gamebook = None,
                glyphs = data.metas.glyphs,
                opening = None,
                clock = ???, // TODO it's in StudyPgnImport, but not here
                crazyData = game.situation.board.crazyData
              )
            )

        (Context(game, context.clocks, context.timeControl), newBranch.some)
      )
      .toOption
      .match
        case Some(branch) => branch
        case None         => (context, None)
