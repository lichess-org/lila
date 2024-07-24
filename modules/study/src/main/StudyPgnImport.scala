package lila.study

import chess.MoveOrDrop.*
import chess.format.pgn.{ Comment as ChessComment, Glyphs, ParsedPgn, PgnNodeData, PgnStr, Tags }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.{ Centis, ErrorStr, Node as PgnNode, Outcome, Status }

import lila.core.LightUser
import lila.tree.Node.{ Comment, Comments, Shapes }
import lila.tree.{ Branch, Branches, ImportResult, Root }

object StudyPgnImport:

  def apply(pgn: PgnStr, contributors: List[LightUser]): Either[ErrorStr, Result] =
    lila.tree.parseImport(pgn).map { case ImportResult(game, result, replay, initialFen, parsedPgn) =>
      val annotator = findAnnotator(parsedPgn, contributors)

      val clock = parsedPgn.tags.clockConfig.map(_.limit)
      parseComments(parsedPgn.initialPosition.comments, annotator) match
        case (shapes, _, _, comments) =>
          val root = Root(
            ply = replay.setup.ply,
            fen = initialFen | game.board.variant.initialFen,
            check = replay.setup.situation.check,
            shapes = shapes,
            comments = comments,
            glyphs = Glyphs.empty,
            clock = clock,
            crazyData = replay.setup.board.crazyData,
            children = parsedPgn.tree
              .fold(Branches.empty)(makeBranches(Context(replay.setup, clock, clock), _, annotator))
          )

          val end = result.map: res =>
            val outcome = Outcome(res.winner)
            End(
              status = res.status,
              outcome = outcome,
              resultText = chess.Outcome.showResult(outcome.some),
              statusText = lila.tree.StatusText(res.status, res.winner, game.board.variant)
            )

          val commented =
            if root.mainline.lastOption.so(_.isCommented) then root
            else
              end.map(endComment).fold(root) { comment =>
                root.updateMainlineLast { _.setComment(comment) }
              }

          Result(
            root = commented,
            variant = game.board.variant,
            tags = PgnTags(parsedPgn.tags),
            end = end
          )
    }

  case class Result(
      root: Root,
      variant: chess.variant.Variant,
      tags: Tags,
      end: Option[End]
  )

  case class End(
      status: Status,
      outcome: Outcome,
      resultText: String,
      statusText: String
  )

  def findAnnotator(pgn: ParsedPgn, contributors: List[LightUser]): Option[Comment.Author] =
    pgn.tags("annotator").map { a =>
      val lowered = a.toLowerCase
      contributors
        .find { c =>
          c.id.value == lowered || c.titleName.toLowerCase == lowered || lowered.endsWith(s"/${c.id}")
        }
        .map { c =>
          Comment.Author.User(c.id, c.titleName)
        }
        .getOrElse(Comment.Author.External(a))
    }

  def endComment(end: End): Comment =
    import end.*
    val text = s"$resultText $statusText"
    Comment(Comment.Id.make, Comment.Text(text), Comment.Author.Lichess)

  def parseComments(
      comments: List[ChessComment],
      annotator: Option[Comment.Author]
  ): (Shapes, Option[Centis], Option[Centis], Comments) =
    comments.foldLeft((Shapes(Nil), none[Centis], none[Centis], Comments(Nil))) {
      case ((shapes, clock, emt, comments), txt) =>
        CommentParser(txt) match
          case CommentParser.ParsedComment(s, c, e, str) =>
            (
              (shapes ++ s),
              c.orElse(clock),
              e.orElse(emt),
              (str.trim match
                case "" => comments
                case com =>
                  comments + Comment(Comment.Id.make, Comment.Text(com), annotator | Comment.Author.Lichess)
              )
            )
    }

  private def makeBranches(
      context: Context,
      node: PgnNode[PgnNodeData],
      annotator: Option[Comment.Author]
  ): Branches =
    val variations =
      node.take(Node.MAX_PLIES).fold(Nil)(_.variations.flatMap(x => makeBranch(context, x.toNode, annotator)))
    removeDuplicatedChildrenFirstNode(
      Branches(makeBranch(context, node, annotator).fold(variations)(_ +: variations))
    )

  private def makeBranch(
      context: Context,
      node: PgnNode[PgnNodeData],
      annotator: Option[Comment.Author]
  ): Option[Branch] =
    try
      node.value
        .san(context.currentGame.situation)
        .fold(
          _ => none, // illegal move; stop here.
          moveOrDrop =>
            val game   = moveOrDrop.applyGame(context.currentGame)
            val uci    = moveOrDrop.toUci
            val sanStr = moveOrDrop.toSanStr
            parseComments(node.value.metas.comments, annotator) match
              case (shapes, clock, emt, comments) =>
                val computedClock = clock.orElse((context.previousClock, emt).mapN(_ - _))
                Branch(
                  id = UciCharPair(uci),
                  ply = game.ply,
                  move = Uci.WithSan(uci, sanStr),
                  fen = Fen.write(game),
                  check = game.situation.check,
                  shapes = shapes,
                  comments = comments,
                  glyphs = node.value.metas.glyphs,
                  clock = computedClock,
                  crazyData = game.situation.board.crazyData,
                  children = node.child.fold(Branches.empty)(
                    makeBranches(Context(game, computedClock, context.currentClock), _, annotator)
                  )
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
