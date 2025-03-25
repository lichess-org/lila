package lila.study

import chess.MoveOrDrop.*
import chess.format.pgn.{ Comment as ChessComment, Glyphs, ParsedPgn, PgnNodeData, PgnStr, Tags, Tag }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.{ ByColor, Centis, ErrorStr, Node as PgnNode, Outcome, Status, TournamentClock, Ply }

import lila.core.LightUser
import lila.tree.Node.{ Comment, Comments, Shapes }
import lila.tree.{ Branch, Branches, ImportResult, Root, Clock }

object StudyPgnImport:

  case class Context(
      currentGame: chess.Game,
      clocks: ByColor[Option[Clock]],
      timeControl: Option[TournamentClock]
  )

  def result(pgn: PgnStr, contributors: List[LightUser]): Either[ErrorStr, Result] =
    lila.tree.parseImport(pgn).map { case ImportResult(game, result, replay, initialFen, parsedPgn) =>
      val annotator = findAnnotator(parsedPgn, contributors)

      val timeControl = parsedPgn.tags.timeControl
      val clock       = timeControl.map(_.limit).map(Clock(_, trust = true.some))
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
            children = parsedPgn.tree.fold(Branches.empty):
              makeBranches(Context(replay.setup, ByColor.fill(clock), timeControl), _, annotator)
          )

          val ending = result.map: res =>
            Ending(
              status = res.status,
              points = res.points,
              resultText = chess.Outcome.showPoints(res.points.some),
              statusText = lila.tree.StatusText(res.status, res.winner, game.board.variant)
            )

          val commented =
            if root.mainline.lastOption.so(_.isCommented) then root
            else
              ending.map(endComment).fold(root) { comment =>
                root.updateMainlineLast { _.setComment(comment) }
              }

          Result(
            root = commented,
            variant = game.board.variant,
            tags = PgnTags
              .withRelevantTags(parsedPgn.tags, Set(Tag.WhiteClock, Tag.BlackClock)),
            ending = ending
          )
    }

  case class Result(
      root: Root,
      variant: chess.variant.Variant,
      tags: Tags,
      ending: Option[Ending]
  )

  case class Ending(
      status: Status,
      points: Outcome.GamePoints,
      resultText: String,
      statusText: String
  )

  def findAnnotator(pgn: ParsedPgn, contributors: List[LightUser]): Option[Comment.Author] =
    pgn.tags("annotator").map { a =>
      val lowered = a.toLowerCase
      contributors
        .find: c =>
          c.id.value == lowered || c.titleName.toLowerCase == lowered || lowered.endsWith(s"/${c.id}")
        .map: c =>
          Comment.Author.User(c.id, c.titleName)
        .getOrElse(Comment.Author.External(a))
    }

  def endComment(end: Ending): Comment =
    import end.*
    val text = s"$resultText $statusText"
    Comment(Comment.Id.make, Comment.Text(text), Comment.Author.Lichess)

  def parseComments(
      comments: List[ChessComment],
      annotator: Option[Comment.Author]
  ): (Shapes, Option[Centis], Option[Centis], Comments) =
    comments.foldLeft((Shapes(Nil), none[Centis], none[Centis], Comments(Nil))):
      case ((shapes, clock, emt, comments), txt) =>
        CommentParser(txt) match
          case CommentParser.ParsedComment(s, c, e, str) =>
            (
              (shapes ++ s),
              c.orElse(clock),
              e.orElse(emt),
              str.trim match
                case "" => comments
                case com =>
                  comments + Comment(Comment.Id.make, Comment.Text(com), annotator | Comment.Author.Lichess)
            )

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
            val game                           = moveOrDrop.applyGame(context.currentGame)
            val uci                            = moveOrDrop.toUci
            val sanStr                         = moveOrDrop.toSanStr
            val (shapes, clock, emt, comments) = parseComments(node.value.metas.comments, annotator)
            val mover                          = !game.ply.turn
            val computedClock: Option[Clock] = clock
              .map(Clock(_, trust = true.some))
              .orElse:
                (context.clocks(mover), emt).mapN(guessNewClockState(_, game.ply, context.timeControl, _))
              .filter(_.positive)
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
              children = node.child.fold(Branches.empty):
                makeBranches(
                  Context(
                    game,
                    context.clocks.update(mover, _ => computedClock),
                    context.timeControl
                  ),
                  _,
                  annotator
                )
            ).some
        )
    catch
      case _: StackOverflowError =>
        logger.warn(s"study PgnImport.makeNode StackOverflowError")
        None

  private def guessNewClockState(prev: Clock, ply: Ply, tc: Option[TournamentClock], emt: Centis): Clock =
    Clock(prev.centis - emt + ~tc.map(_.incrementAtPly(ply)), trust = false.some)

  /*
   * Fix bad PGN like this one found on reddit:
   * 7. c4 (7. c4 Nf6) (7. c4 dxc4) 7... cxd4
   * where 7. c4 appears three times
   */
  // TODO this could probably be refactored better or moved to scalachess
  private def removeDuplicatedChildrenFirstNode(children: Branches): Branches =
    children.first match
      case Some(main) if children.variations.exists(_.id == main.id) =>
        Branches:
          main +: children.variations.flatMap { node =>
            if node.id == main.id then node.children.nodes
            else List(node)
          }
      case _ => children
