package lila.study

import chess.{ Centis, ErrorStr, Node as PgnNode }
import chess.format.pgn.{ Glyphs, ParsedPgn, San, Tags, PgnStr, PgnNodeData, Comment as ChessComment }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.MoveOrDrop.*

import lila.common.LightUser
import lila.importer.{ ImportData, Preprocessed }
import lila.tree.{ Root, Branch, Branches }
import lila.tree.Node.{ Comment, Comments, Shapes }

object PgnImport:

  case class Result(
      root: Root,
      variant: chess.variant.Variant,
      tags: Tags,
      end: Option[End]
  )

  case class End(
      status: chess.Status,
      winner: Option[chess.Color],
      resultText: String,
      statusText: String
  )

  def apply(pgn: PgnStr, contributors: List[LightUser]): Either[ErrorStr, Result] =
    ImportData(pgn, analyse = none).preprocess(user = none).map {
      case Preprocessed(game, replay, initialFen, parsedPgn) =>
        val annotator = findAnnotator(parsedPgn, contributors)
        parseComments(parsedPgn.initialPosition.comments, annotator) match
          case (shapes, _, comments) =>
            val root = Root(
              ply = replay.setup.ply,
              fen = initialFen | game.variant.initialFen,
              check = replay.setup.situation.check,
              shapes = shapes,
              comments = comments,
              glyphs = Glyphs.empty,
              clock = parsedPgn.tags.clockConfig.map(_.limit),
              crazyData = replay.setup.situation.board.crazyData,
              children = parsedPgn.tree.fold(Branches.empty)(makeBranches(replay.setup, _, annotator))
            )
            val end: Option[End] = (game.finished option game.status).map { status =>
              End(
                status = status,
                winner = game.winnerColor,
                resultText = chess.Outcome.showResult(chess.Outcome(game.winnerColor).some),
                statusText = lila.game.StatusText(status, game.winnerColor, game.variant)
              )
            }
            val commented =
              if root.mainline.lastOption.so(_.isCommented) then root
              else
                end.map(endComment).fold(root) { comment =>
                  root updateMainlineLast { _.setComment(comment) }
                }
            Result(
              root = commented,
              variant = game.variant,
              tags = PgnTags(parsedPgn.tags),
              end = end
            )
    }

  def findAnnotator(pgn: ParsedPgn, contributors: List[LightUser]): Option[Comment.Author] =
    pgn tags "annotator" map { a =>
      val lowered = a.toLowerCase
      contributors.find { c =>
        c.id.value == lowered || c.titleName.toLowerCase == lowered || lowered.endsWith(s"/${c.id}")
      } map { c =>
        Comment.Author.User(c.id, c.titleName)
      } getOrElse Comment.Author.External(a)
    }

  def endComment(end: End): Comment =
    import end.*
    val text = s"$resultText $statusText"
    Comment(Comment.Id.make, Comment.Text(text), Comment.Author.Lichess)

  def parseComments(
      comments: List[ChessComment],
      annotator: Option[Comment.Author]
  ): (Shapes, Option[Centis], Comments) =
    comments.foldLeft((Shapes(Nil), none[Centis], Comments(Nil))) { case ((shapes, clock, comments), txt) =>
      CommentParser(txt) match
        case CommentParser.ParsedComment(s, c, str) =>
          (
            (shapes ++ s),
            c orElse clock,
            (str.trim match
              case "" => comments
              case com =>
                comments + Comment(Comment.Id.make, Comment.Text(com), annotator | Comment.Author.Lichess)
            )
          )
    }

  private def makeBranches(
      prev: chess.Game,
      node: PgnNode[PgnNodeData],
      annotator: Option[Comment.Author]
  ): Branches =
    val variations = node.take(Node.MAX_PLIES).variations.flatMap(x => makeBranch(prev, x.toNode, annotator))
    removeDuplicatedChildrenFirstNode(
      Branches(makeBranch(prev, node, annotator).fold(variations)(_ +: variations))
    )

  private def makeBranch(
      prev: chess.Game,
      node: PgnNode[PgnNodeData],
      annotator: Option[Comment.Author]
  ): Option[Branch] =
    try
      node.value
        .san(prev.situation)
        .fold(
          _ => none, // illegal move; stop here.
          moveOrDrop =>
            val game   = moveOrDrop.applyGame(prev)
            val uci    = moveOrDrop.toUci
            val sanStr = moveOrDrop.toSanStr
            parseComments(node.value.metas.comments, annotator) match
              case (shapes, clock, comments) =>
                Branch(
                  id = UciCharPair(uci),
                  ply = game.ply,
                  move = Uci.WithSan(uci, sanStr),
                  fen = Fen write game,
                  check = game.situation.check,
                  shapes = shapes,
                  comments = comments,
                  glyphs = node.value.metas.glyphs,
                  clock = clock,
                  crazyData = game.situation.board.crazyData,
                  children = node.child.fold(Branches.empty)(makeBranches(game, _, annotator))
                ).some
        )
    catch
      case _: StackOverflowError =>
        logger.warn(s"study PgnImport.makeNode StackOverflowError")
        None

  /*
   * Fix bad PGN like this one found on reddit:
   * 7. c4 (7. c4 Nf6) (7. c4 dxc4) 7... cxd4
   * where 7. c4 appears three times
   */
  // TODO this could probably be refactored better or moved to scalachess
  private def removeDuplicatedChildrenFirstNode(children: Branches): Branches =
    children.first match
      case Some(main) if children.variations.exists(_.id == main.id) =>
        Branches {
          main +: children.variations.flatMap { node =>
            if node.id == main.id then node.children.nodes
            else List(node)
          }
        }
      case _ => children
