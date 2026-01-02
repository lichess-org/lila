package lila.study

import chess.format.pgn.{ Comment as CommentStr, Glyphs, ParsedPgn, PgnNodeData, PgnStr, Tags, Tag }
import chess.format.{ Fen, Uci, UciCharPair }
import chess.{ ByColor, Centis, ErrorStr, Node as PgnNode, Outcome, Status, TournamentClock, Ply }

import lila.core.LightUser
import lila.tree.Node.{ Comment, Comments, Shapes }
import lila.tree.{ Branch, Branches, ImportResult, ParseImport, Root, Clock }

object StudyPgnImport:

  case class Context(
      currentPosition: chess.Position,
      clocks: ByColor[Option[Clock]],
      timeControl: Option[TournamentClock],
      ply: Ply
  )

  def result(pgn: PgnStr, contributors: List[LightUser]): Either[ErrorStr, Result] =
    if pgn.value.sizeIs > 100_000 then Left(ErrorStr("PGN too large"))
    else
      for
        parsed <- ParseImport.full(pgn)
        full = result(parsed, contributors)
        valid <-
          if full.root.children.countRecursive > Chapter.maxNodes
          then Left(ErrorStr("PGN has too many moves/nodes"))
          else Right(full)
      yield valid

  def result(importResult: ImportResult, contributors: List[LightUser]): Result =
    import importResult.{ replay, initialFen, parsed }
    val annotator = findAnnotator(parsed, contributors)

    val timeControl = parsed.tags.timeControl
    val clock = timeControl.map(_.limit).map(Clock(_, trust = true.some))
    parseComments(parsed.initialPosition.comments, annotator) match
      case (shapes, _, _, comments) =>
        val root = Root(
          ply = replay.setup.ply,
          fen = initialFen | replay.setup.position.variant.initialFen,
          check = replay.setup.position.check,
          shapes = shapes,
          comments = comments,
          glyphs = Glyphs.empty,
          clock = clock,
          crazyData = replay.setup.position.crazyData,
          children = parsed.tree.fold(Branches.empty):
            makeBranches(
              Context(replay.setup.position, ByColor.fill(clock), timeControl, replay.setup.ply),
              _,
              annotator
            )
        )

        val ending = importResult.result.map: res =>
          Ending(
            status = res.status,
            points = res.points,
            resultText = chess.Outcome.showPoints(res.points.some),
            statusText = lila.tree.StatusText(res.status, res.winner, replay.setup.position.variant)
          )

        val commented =
          if root.mainline.lastOption.so(_.isCommented) then root
          else
            ending.map(endComment).fold(root) { comment =>
              root.updateMainlineLast { _.setComment(comment) }
            }

        Result(
          root = commented,
          variant = replay.setup.position.variant,
          tags = StudyPgnTags
            .withRelevantTags(parsed.tags, Set(Tag.WhiteClock, Tag.BlackClock)),
          ending = ending,
          chapterNameHint = parsed.tags("ChapterName").map(_.trim).filter(_.nonEmpty)
        )

  case class Result(
      root: Root,
      variant: chess.variant.Variant,
      tags: Tags,
      ending: Option[Ending],
      chapterNameHint: Option[String]
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
    Comment(Comment.Id.make, CommentStr(text), Comment.Author.Lichess)

  def parseComments(
      comments: List[CommentStr],
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
              str.trimNonEmpty.fold(comments): com =>
                comments + Comment(Comment.Id.make, com, annotator | Comment.Author.Lichess)
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
        .san(context.currentPosition)
        .fold(
          _ => none, // illegal move; stop here.
          moveOrDrop =>
            val position = moveOrDrop.after
            val currentPly = context.ply.next
            val uci = moveOrDrop.toUci
            val sanStr = moveOrDrop.toSanStr
            val (shapes, clock, emt, comments) = parseComments(node.value.metas.comments, annotator)
            val mover = !position.color
            val computedClock: Option[Clock] = clock
              .map(Clock(_, trust = true.some))
              .orElse:
                (context.clocks(mover), emt).mapN(guessNewClockState(_, context.timeControl, _))
              .filter(_.positive)
            Branch(
              id = UciCharPair(uci),
              ply = currentPly,
              move = Uci.WithSan(uci, sanStr),
              fen = Fen.write(position, currentPly.fullMoveNumber),
              check = position.check,
              shapes = shapes,
              comments = comments,
              glyphs = node.value.metas.glyphs,
              clock = computedClock,
              crazyData = position.crazyData,
              children = node.child.fold(Branches.empty):
                makeBranches(
                  Context(
                    position,
                    context.clocks.update(mover, _ => computedClock),
                    context.timeControl,
                    currentPly
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

  private[study] def guessNewClockState(
      prev: Clock,
      tc: Option[TournamentClock],
      emt: Centis
  ): Clock =
    Clock(prev.centis - emt + tc.so(_.increment), trust = false.some)

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
