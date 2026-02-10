package lila.study

import akka.stream.scaladsl.*
import chess.format.pgn as chessPgn
import chess.format.pgn.{ Comment, Glyphs, InitialComments, Pgn, PgnStr, PgnTree, Tag, Tags }
import scalalib.StringOps.slug

import lila.tree.Node.{ Shape, Shapes }
import lila.tree.{ Analysis, Metas, NewBranch, NewRoot, NewTree, Root }

final class PgnDump(
    chapterRepo: ChapterRepo,
    analyser: lila.tree.Analyser,
    annotator: lila.tree.Annotator,
    lightUserApi: lila.core.user.LightUserApi,
    net: lila.core.config.NetConfig
)(using Executor):

  import PgnDump.*

  def chaptersOf(study: Study, flags: Chapter => WithFlags): Source[PgnStr, ?] =
    chapterRepo
      .orderedByStudySource(study.id)
      .mapAsync(1)(chapter => ofChapter(study, flags(chapter))(chapter))

  def ofFirstChapter(study: Study, flags: WithFlags): Fu[Option[PgnStr]] =
    chapterRepo
      .firstByStudy(study.id)
      .flatMapz: chapter =>
        ofChapter(study, flags)(chapter).map(some)

  def ofChapter(study: Study, flags: WithFlags)(chapter: Chapter): Fu[PgnStr] =
    (flags.comments && chapter.serverEval.exists(_.done))
      .so(analyser.byId(Analysis.Id(study.id, chapter.id)))
      .map(ofChapter(study, flags)(chapter, _))

  private val fileR = """[\s,]""".r
  private val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")

  def ownerName(study: Study) = lightUserApi.sync(study.ownerId).fold(study.ownerId)(_.name)

  def filename(study: Study): String =
    val date = dateFormatter.print(study.createdAt)
    fileR.replaceAllIn(
      if study.isRelay
      then s"lichess_broadcast_${slug(study.name.value)}_$date"
      else s"lichess_study_${slug(study.name.value)}_by_${ownerName(study)}_$date",
      ""
    )

  def filename(study: Study, chapter: Chapter): String =
    val date = dateFormatter.print(chapter.createdAt)
    fileR.replaceAllIn(
      if study.isRelay
      then s"lichess_broadcast_${slug(study.name.value)}_${slug(chapter.name.value)}_$date"
      else
        s"lichess_study_${slug(study.name.value)}_${slug(chapter.name.value)}_by_${ownerName(study)}_$date"
      ,
      ""
    )

  private def makeTags(study: Study, chapter: Chapter)(using flags: WithFlags): Tags =
    flags.updateTags:
      Tags:
        val opening = chapter.opening
        val genTags = List(
          Tag(_.Event, s"${study.name}: ${chapter.name}"),
          Tag(_.Variant, chapter.setup.variant.name.capitalize),
          Tag(_.ECO, opening.fold("?")(_.eco)),
          Tag(_.Opening, opening.fold("?")(_.name)),
          Tag(_.Result, "*") // required for SCID to import
        ) ::: study.isRelay.not.so(
          List(
            Tag("StudyName", study.name),
            Tag("ChapterName", chapter.name),
            Tag("ChapterURL", s"${net.baseUrl}/study/${study.id}/${chapter.id}"),
            Tag(_.Annotator, s"${net.baseUrl}/@/${ownerName(study)}")
          )
        ) ::: chapter.root.fen.isInitial.not.so(
          List(
            Tag(_.FEN, chapter.root.fen.value),
            Tag("SetUp", "1")
          )
        ) ::: (!chapter.tags.exists(_.Date)).so {
          val dateStr = Tag.UTCDate.format.print(chapter.createdAt)
          List(
            Tag(_.Date, dateStr),
            Tag(_.UTCDate, dateStr),
            Tag(_.UTCTime, Tag.UTCTime.format.print(chapter.createdAt))
          )
        } ::: List(
          flags.orientation.option(Tag("Orientation", chapter.setup.orientation.name)),
          chapter.isGamebook.option(Tag("ChapterMode", "gamebook"))
        ).flatten
        genTags
          .foldLeft(chapter.tagsExport.value.reverse): (tags, tag) =>
            if tags.exists(t => tag.name == t.name) && tag.name != Tag.FEN
            then tags
            else tag :: tags
          .reverse

  private def ofChapter(study: Study, flags: WithFlags)(
      chapter: Chapter,
      analysis: Option[Analysis]
  ): PgnStr =
    val tags = makeTags(study, chapter)(using flags)
    val pgn = rootToPgn(chapter.root, tags)(using flags)
    annotator.toPgnString(analysis.fold(pgn)(annotator.addEvals(pgn, _)))

object PgnDump:

  case class WithFlags(
      comments: Boolean,
      variations: Boolean,
      clocks: Boolean,
      orientation: Boolean,
      updateTags: Update[Tags] = identity
  )
  val fullFlags = WithFlags(true, true, true, true)
  val withoutOrientation = fullFlags.copy(orientation = false)

  def rootToPgn(root: Root, tags: Tags, comments: InitialComments)(using WithFlags): Pgn =
    rootToPgn(NewRoot(root), tags, comments)

  def rootToPgn(root: Root, tags: Tags)(using WithFlags): Pgn =
    rootToPgn(NewRoot(root), tags)

  def rootToPgn(root: NewRoot, tags: Tags)(using flags: WithFlags): Pgn =
    val comments =
      if flags.comments then InitialComments(root.metas.commentWithShapes)
      else InitialComments.empty
    rootToPgn(root, tags, comments)

  def rootToPgn(root: NewRoot, tags: Tags, comments: InitialComments)(using WithFlags): Pgn =
    Pgn(tags, comments, root.tree.map(treeToTree), root.ply.next)

  def treeToTree(tree: NewTree)(using flags: WithFlags): PgnTree =
    if flags.variations then tree.map(branchToMove) else tree.mapMainline(branchToMove)

  private def branchToMove(node: NewBranch)(using flags: WithFlags) =
    chessPgn.Move(
      san = node.move.san,
      glyphs = flags.comments.so(node.metas.glyphs),
      comments = flags.comments.so(node.metas.commentWithShapes),
      opening = none,
      result = none,
      timeLeft = flags.clocks.so(node.clock.map(_.centis.roundSeconds))
    )

  extension (metas: Metas)
    def commentWithShapes: List[Comment] =
      metas.comments.value.map(_.text.into(Comment)) ::: shapeComment(metas.shapes).toList

  // [%csl Gb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]
  private def shapeComment(shapes: Shapes): Option[Comment] =
    def render(as: String)(shapes: List[String]) =
      shapes match
        case Nil => ""
        case shapes => s"[%$as ${shapes.mkString(",")}]"
    val circles = render("csl"):
      shapes.value.collect { case Shape.Circle(brush, orig) =>
        s"${brush.head.toUpper}${orig.key}"
      }
    val arrows = render("cal"):
      shapes.value.collect { case Shape.Arrow(brush, orig, dest) =>
        s"${brush.head.toUpper}${orig.key}${dest.key}"
      }
    Comment.from(s"$circles$arrows".nonEmptyOption)
