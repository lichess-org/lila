package lila.study

import monocle.syntax.all.*
import chess.{ Centis, ErrorStr, Node as PgnNode, Outcome }
import chess.format.pgn.{
  Glyphs,
  ParsedPgn,
  ParsedPgnTree,
  San,
  Tags,
  PgnStr,
  PgnNodeData,
  Comment as ChessComment
}
import chess.format.{ Fen, Uci, UciCharPair, UciPath }
import chess.MoveOrDrop.*
import lila.tree.Node.{ Comment, Comments, Shapes }
import lila.common.LightUser
import lila.importer.{ ImportData, Preprocessed }
import lila.tree.{ NewRoot, NewTree, NewBranch, Metas }

object NewPgnImport:

  case class Result(
      root: NewRoot,
      variant: chess.variant.Variant,
      tags: Tags,
      end: Option[PgnImport.End]
  )

  def apply(pgn: PgnStr, contributors: List[LightUser]): Either[ErrorStr, Result] =
    ImportData(pgn, analyse = none).preprocess(user = none).map {
      case Preprocessed(game, replay, initialFen, parsedPgn) =>
        val annotator = PgnImport.findAnnotator(parsedPgn, contributors)
        PgnImport.parseComments(parsedPgn.initialPosition.comments, annotator) match
          case (shapes, _, comments) =>
            val root: NewRoot =
              NewRoot(
                Metas(
                  ply = replay.setup.ply,
                  fen = initialFen | game.variant.initialFen,
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
                  clock = parsedPgn.tags.clockConfig.map(_.limit)
                ),
                parsedPgn.tree.flatMap(makeTree(replay.setup, _, annotator))
              )
            val end: Option[PgnImport.End] = (game.finished option game.status).map { status =>
              PgnImport.End(
                status = status,
                outcome = Outcome(game.winnerColor),
                resultText = chess.Outcome.showResult(chess.Outcome(game.winnerColor).some),
                statusText = lila.game.StatusText(status, game.winnerColor, game.variant)
              )
            }
            val commented =
              if root.tree.map(_.lastMainlineNode).exists(_.value.metas.comments.value.nonEmpty) then root
              else
                end.map(PgnImport.endComment).fold(root) { comment =>
                  root
                    .focus(_.tree.some)
                    .modify(_.modifyLastMainlineNode(_.focus(_.value.metas.comments).modify(_ + comment)))
                }
            Result(
              root = root,
              variant = game.variant,
              tags = PgnTags(parsedPgn.tags),
              end = end
            )
    }

  private def makeTree(
      setup: chess.Game,
      node: ParsedPgnTree,
      annotator: Option[Comment.Author]
  ): Option[PgnNode[NewBranch]] =
    node.mapAccumlOption_(setup): (setup, data) =>
      transform(setup, data, annotator)

  private def transform(
      context: chess.Game,
      data: PgnNodeData,
      annotator: Option[Comment.Author]
  ): (chess.Game, Option[NewBranch]) =
    data
      .san(context.situation)
      .map(moveOrDrop =>
        val game   = moveOrDrop.applyGame(context)
        val uci    = moveOrDrop.toUci
        val id     = UciCharPair(uci)
        val sanStr = moveOrDrop.toSanStr
        (
          game,
          PgnImport.parseComments(data.metas.comments, annotator) match
            case (shapes, clock, comments) =>
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
                  clock = clock,
                  crazyData = game.situation.board.crazyData
                )
              ).some
        )
      )
      .toOption match
      case Some(branch) => branch
      case None         => (context, None)
