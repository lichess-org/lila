package lila.study

import akka.stream.scaladsl.*
import chess.format.pgn.{ Glyphs, InitialComments, Pgn, Tag, Tags, PgnStr, Comment, PgnTree }
import chess.format.{ pgn as chessPgn }

import lila.common.String.slugify
import lila.tree.{ Root, Branch, Branches, NewBranch, NewTree, NewRoot }
import lila.tree.Node.{ Shape, Shapes }
import lila.analyse.Analysis

final class PgnDump(
    chapterRepo: ChapterRepo,
    analyser: lila.analyse.Analyser,
    annotator: lila.analyse.Annotator,
    lightUserApi: lila.user.LightUserApi,
    net: lila.common.config.NetConfig
)(using Executor):

  import PgnDump.*

  def chaptersOf(study: Study, flags: WithFlags): Source[PgnStr, ?] =
    chapterRepo
      .orderedByStudySource(study.id)
      .mapAsync(1)(ofChapter(study, flags))

  def ofFirstChapter(study: Study, flags: WithFlags): Fu[Option[PgnStr]] =
    chapterRepo
      .firstByStudy(study.id)
      .flatMapz: chapter =>
        ofChapter(study, flags)(chapter).map(some)

  def ofChapter(study: Study, flags: WithFlags)(chapter: Chapter): Fu[PgnStr] =
    chapter.serverEval.exists(_.done) so analyser.byId(Analysis.Id(study.id, chapter.id)) map { analysis =>
      ofChapter(study, flags)(chapter, analysis)
    }

  private val fileR         = """[\s,]""".r
  private val dateFormatter = java.time.format.DateTimeFormatter ofPattern "yyyy.MM.dd"

  def ownerName(study: Study) = lightUserApi.sync(study.ownerId).fold(study.ownerId)(_.name)

  def filename(study: Study): String =
    val date = dateFormatter.print(study.createdAt)
    fileR.replaceAllIn(
      s"lichess_study_${slugify(study.name.value)}_by_${ownerName(study)}_$date",
      ""
    )

  def filename(study: Study, chapter: Chapter): String =
    val date = dateFormatter.print(chapter.createdAt)
    fileR.replaceAllIn(
      s"lichess_study_${slugify(study.name.value)}_${slugify(chapter.name.value)}_by_${ownerName(study)}_$date",
      ""
    )

  private def chapterUrl(studyId: StudyId, chapterId: StudyChapterId) =
    s"${net.baseUrl}/study/$studyId/$chapterId"

  private def annotatorTag(study: Study) =
    Tag(_.Annotator, s"${net.baseUrl}/@/${ownerName(study)}")

  private def makeTags(study: Study, chapter: Chapter)(using flags: WithFlags): Tags =
    Tags:
      val opening = chapter.opening
      val genTags = List(
        Tag(_.Event, s"${study.name}: ${chapter.name}"),
        Tag(_.Site, chapterUrl(study.id, chapter.id)),
        Tag(_.UTCDate, Tag.UTCDate.format.print(chapter.createdAt)),
        Tag(_.UTCTime, Tag.UTCTime.format.print(chapter.createdAt)),
        Tag(_.Variant, chapter.setup.variant.name.capitalize),
        Tag(_.ECO, opening.fold("?")(_.eco)),
        Tag(_.Opening, opening.fold("?")(_.name)),
        Tag(_.Result, "*"), // required for SCID to import
        annotatorTag(study)
      ) ::: (!chapter.root.fen.isInitial).so(
        List(
          Tag(_.FEN, chapter.root.fen.value),
          Tag("SetUp", "1")
        )
      ) :::
        flags.source.so(List(Tag("Source", chapterUrl(study.id, chapter.id)))) :::
        flags.orientation.so(List(Tag("Orientation", chapter.setup.orientation.name))) :::
        chapter.isGamebook.so(List(Tag("ChapterMode", "gamebook")))
      genTags
        .foldLeft(chapter.tags.value.reverse): (tags, tag) =>
          if tags.exists(t => tag.name == t.name) then tags
          else tag :: tags
        .reverse

  def ofChapter(study: Study, flags: WithFlags)(chapter: Chapter, analysis: Option[Analysis]): PgnStr =
    val root = chapter.root
    val tags = makeTags(study, chapter)(using flags)
    val pgn  = rootToPgn(root, tags)(using flags)
    annotator toPgnString analysis.fold(pgn)(annotator.addEvals(pgn, _))

object PgnDump:

  case class WithFlags(
      comments: Boolean,
      variations: Boolean,
      clocks: Boolean,
      source: Boolean,
      orientation: Boolean
  )
  val fullFlags = WithFlags(true, true, true, true, true)

  private type Variations = List[Branch]
  private val noVariations: Variations = Nil

  def rootToPgn(root: Root, tags: Tags)(using WithFlags): Pgn =
    Pgn(
      tags,
      InitialComments(root.comments.value.map(_.text into Comment) ::: shapeComment(root.shapes).toList),
      rootToTree(root)
    )

  def rootToTree(root: Root)(using WithFlags): Option[PgnTree] =
    NewTree(root).map(treeToTree)

  def treeToTree(tree: NewTree)(using flags: WithFlags): PgnTree =
    if flags.variations then tree.map(branchToMove) else tree.mapMainline(branchToMove)

  private def branchToMove(node: NewBranch)(using flags: WithFlags) =
    chessPgn.Move(
      node.ply,
      san = node.move.san,
      glyphs = if flags.comments then node.metas.glyphs else Glyphs.empty,
      comments = flags.comments.so:
        node.comments.value.map(_.text into Comment) ::: shapeComment(node.shapes).toList
      ,
      opening = none,
      result = none,
      secondsLeft = flags.clocks so node.clock.map(_.roundSeconds)
    )

  // [%csl Gb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]
  private def shapeComment(shapes: Shapes): Option[Comment] =
    def render(as: String)(shapes: List[String]) =
      shapes match
        case Nil    => ""
        case shapes => s"[%$as ${shapes.mkString(",")}]"
    val circles = render("csl"):
      shapes.value.collect { case Shape.Circle(brush, orig) =>
        s"${brush.head.toUpper}${orig.key}"
      }
    val arrows = render("cal"):
      shapes.value.collect { case Shape.Arrow(brush, orig, dest) =>
        s"${brush.head.toUpper}${orig.key}${dest.key}"
      }
    Comment from s"$circles$arrows".some.filter(_.nonEmpty)
