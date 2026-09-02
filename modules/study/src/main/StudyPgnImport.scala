package lila.study

import chess.format.pgn.{ Comment as CommentStr, ParsedPgn, PgnNodeData, PgnStr, Tags }
import chess.format.{ Fen, Uci }
import chess.{ ByColor, Centis, ErrorStr, Node as PgnNode, Outcome, Status, TournamentClock, Ply }

import lila.core.LightUser
import lila.tree.Node.{ Comment, Comments, Shapes, Glyphs }
import lila.tree.{ Branch, Branches, ImportResult, ParseImport, Root, Clock }

object StudyPgnImport:

  case class Annotators(default: Option[Comment.Author], known: Map[UserId, Comment.Author]):
    def resolve(author: CommentParser.Author): Comment.Author =
      author.accountId
        .flatMap(known.get)
        .orElse(byProfileUrl(author.name))
        .getOrElse(Comment.Author.External(author.name))

    // an [%anno] holding a profile URL is what a study export of a study export looks like,
    // and the Annotator tag it came from used to resolve the same way
    private def byProfileUrl(name: String): Option[Comment.Author] =
      val lowered = name.toLowerCase
      known.collectFirst:
        case (id, author) if lowered.endsWith(s"/$id") => author

  object Annotators:
    def apply(default: Option[Comment.Author], contributors: List[LightUser]): Annotators =
      Annotators(default, contributors.view.map(u => u.id -> Comment.author(u)).toMap)

  case class Context(
      currentPosition: chess.Position,
      clocks: ByColor[Option[Clock]],
      timeControl: Option[TournamentClock],
      ply: Ply
  )

  def result(
      pgn: PgnStr,
      contributors: List[LightUser],
      strict: Boolean = false,
      importer: Option[LightUser] = None
  ): Either[ErrorStr, Result] =
    if pgn.value.sizeIs > 100_000 then Left(ErrorStr("PGN too large"))
    else
      for
        parsed <- ParseImport.full(pgn)
        full = result(parsed, contributors, importer)
        valid <-
          if full.root.children.countRecursive > Chapter.maxNodes
          then Left(ErrorStr("PGN has too many moves/nodes"))
          else if strict then parsed.replayError.toLeft(full)
          else Right(full)
      yield valid

  def result(importResult: ImportResult, contributors: List[LightUser]): Result =
    result(importResult, contributors, None)

  def result(importResult: ImportResult, contributors: List[LightUser], importer: Option[LightUser]): Result =
    import importResult.{ replay, initialFen, parsed }
    val annotator = findAnnotator(parsed, contributors).orElse(importer.map(Comment.author))
    val annotators = Annotators(annotator, contributors ::: importer.toList)

    val timeControl = parsed.tags.timeControl
    val clock = timeControl.map(_.limit).map(Clock(_, trust = true.some))
    parseComments(parsed.initialPosition.comments, annotators) match
      case (shapes, _, _, comments) =>
        val root = Root(
          ply = replay.setup.ply,
          fen = initialFen | replay.setup.position.variant.initialFen,
          shapes = shapes,
          comments = comments,
          glyphs = Glyphs.empty,
          clock = clock,
          crazyData = replay.setup.position.crazyData,
          children = parsed.tree.fold(Branches.empty):
            makeBranches(
              Context(replay.setup.position, ByColor.fill(clock), timeControl, replay.setup.ply),
              _,
              annotators
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
            .withRelevantTags(
              parsed.tags,
              StudyPgnTags.clockTags,
              replay.setup.position.variant
            ),
          ending = ending,
          chapterNameHint = StudyChapterName.from(parsed.tags("ChapterName").map(_.trim).filter(_.nonEmpty))
        )

  case class Result(
      root: Root,
      variant: chess.variant.Variant,
      tags: Tags,
      ending: Option[Ending],
      chapterNameHint: Option[StudyChapterName]
  )

  case class Ending(
      status: Status,
      points: Outcome.GamePoints,
      resultText: String,
      statusText: String
  )

  def findAnnotator(pgn: ParsedPgn, contributors: List[LightUser]): Option[Comment.Author] =
    pgn.tags("annotator").map { a =>
      contributors
        .find(c => annotatorMatches(a, c.id, c.titleName))
        .fold(Comment.Author.External(a))(Comment.author)
    }

  def annotatorMatches(annotator: String, id: UserId, name: String): Boolean =
    val lowered = annotator.toLowerCase
    id.value == lowered || name.toLowerCase == lowered || lowered.endsWith(s"/$id")

  def endComment(end: Ending): Comment =
    import end.*
    val text = s"$resultText $statusText"
    Comment(Comment.Id.make, CommentStr(text), Comment.Author.Lichess)

  def parseComments(
      comments: List[CommentStr],
      annotators: Annotators
  ): (Shapes, Option[Centis], Option[Centis], Comments) =
    comments.foldRight((Shapes(Nil), none[Centis], none[Centis], Comments(Nil))):
      case (txt, (shapes, clock, emt, comments)) =>
        CommentParser(txt) match
          case CommentParser.ParsedComment(s, c, e, str) =>
            (
              (shapes ++ s),
              c.orElse(clock),
              e.orElse(emt),
              str.trimNonEmpty.fold(comments): text =>
                val author = CommentParser
                  .author(txt)
                  .map(annotators.resolve)
                  .orElse(annotators.default) | Comment.Author.Lichess
                comments
                  .findBy(author)
                  .fold(comments + Comment(Comment.Id.make, text, author)): existing =>
                    comments.set(existing.copy(text = CommentStr(s"$text\n${existing.text}")))
            )

  private def makeBranches(
      context: Context,
      node: PgnNode[PgnNodeData],
      annotators: Annotators
  ): Branches =
    val variations =
      node
        .take(Node.MAX_PLIES)
        .fold(Nil)(_.variations.flatMap(x => makeBranch(context, x.toNode, annotators)))
    mergeDuplicateVariations(
      Branches(makeBranch(context, node, annotators).fold(variations)(_ +: variations))
    )

  private def makeBranch(
      context: Context,
      node: PgnNode[PgnNodeData],
      annotators: Annotators
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
            val (shapes, clock, emt, comments) = parseComments(node.value.metas.comments, annotators)
            val mover = !position.color
            val computedClock: Option[Clock] = clock
              .map(Clock(_, trust = true.some))
              .orElse:
                (context.clocks(mover), emt).mapN(guessNewClockState(_, context.timeControl, _))
              .filter(_.positive)
            Branch(
              ply = currentPly,
              move = Uci.WithSan(uci, sanStr),
              fen = Fen.write(position, currentPly.fullMoveNumber),
              shapes = shapes,
              comments = comments,
              glyphs = Glyphs.fromBase(node.value.metas.glyphs),
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
                  annotators
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

  private def mergeDuplicateVariations(children: Branches): Branches =
    val list = children.toList
    if list.sizeIs < 2 then children
    else
      val ids = list.map(_.id).distinct
      if ids.sizeCompare(list) == 0 then children
      else
        val deduplicated = ids.flatMap: id =>
          val matching = list.filter(_.id == id)
          matching.headOption.map: main =>
            val mergedChildrenList = matching.flatMap(_.children.toList)
            main.copy(children = mergeDuplicateVariations(Branches(mergedChildrenList)))

        Branches(deduplicated)
