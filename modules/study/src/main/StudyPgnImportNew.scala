package lila.study

import chess.MoveOrDrop.*
import chess.format.pgn.{ Glyphs, ParsedPgn, ParsedPgnTree, PgnNodeData, PgnStr, Tags }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.{ Centis, ErrorStr, Node as PgnNode, Outcome }
import monocle.syntax.all.*

import lila.core.LightUser
import lila.tree.Node.{ Comment, Comments }
import lila.tree.{ ImportResult, Metas, NewBranch, NewRoot, NewTree }

case class Context(
    currentGame: chess.Game,
    currentClock: Option[Centis],
    previousClock: Option[Centis]
)

object StudyPgnImportNew:

  case class Result(
      root: NewRoot,
      variant: chess.variant.Variant,
      tags: Tags,
      end: Option[StudyPgnImport.End]
  )

  def apply(pgn: PgnStr, contributors: List[LightUser]): Either[ErrorStr, Result] =
    lila.tree.parseImport(pgn).map { case ImportResult(game, result, replay, initialFen, parsedPgn) =>
      val annotator = StudyPgnImport.findAnnotator(parsedPgn, contributors)
      StudyPgnImport.parseComments(parsedPgn.initialPosition.comments, annotator) match
        case (shapes, _, _, comments) =>
          val clock = parsedPgn.tags.clockConfig.map(_.limit)
          val setup = Context(replay.setup, clock, clock)
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

          val end = result.map: res =>
            val outcome = Outcome(res.winner)
            StudyPgnImport.End(
              status = res.status,
              outcome = outcome,
              resultText = chess.Outcome.showResult(outcome.some),
              statusText = lila.tree.StatusText(res.status, res.winner, game.board.variant)
            )

          val commented =
            if root.tree.map(_.lastMainlineNode).exists(_.value.metas.comments.value.nonEmpty) then root
            else
              end.map(StudyPgnImport.endComment).fold(root) { comment =>
                root
                  .focus(_.tree.some)
                  .modify(_.modifyLastMainlineNode(_.focus(_.value.metas.comments).modify(_ + comment)))
              }
          Result(
            root = commented,
            variant = game.board.variant,
            tags = PgnTags(parsedPgn.tags),
            end = end
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
                clock = clock.orElse((context.previousClock, emt).mapN(_ - _)),
                crazyData = game.situation.board.crazyData
              )
            )

        (Context(game, newBranch.metas.clock, context.currentClock), newBranch.some)
      )
      .toOption
      .match
        case Some(branch) => branch
        case None         => (context, None)
