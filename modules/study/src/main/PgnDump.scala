package lila.study

import akka.stream.scaladsl.*
import chess.format.pgn.{ Glyphs, Initial, Pgn, Tag, Tags, PgnStr }
import chess.format.{ pgn as chessPgn }
import scala.concurrent.duration.*

import lila.common.String.slugify
import lila.tree.Node.{ Shape, Shapes }

final class PgnDump(
    chapterRepo: ChapterRepo,
    analyser: lila.analyse.Analyser,
    annotator: lila.analyse.Annotator,
    lightUserApi: lila.user.LightUserApi,
    net: lila.common.config.NetConfig
)(using Executor):

  import PgnDump.*

  def apply(study: Study, flags: WithFlags): Source[PgnStr, ?] =
    chapterRepo
      .orderedByStudySource(study.id)
      .throttle(16, 1 second)
      .mapAsync(1)(ofChapter(study, flags))

  def ofChapter(study: Study, flags: WithFlags)(chapter: Chapter): Fu[PgnStr] =
    chapter.serverEval.exists(_.done) ?? analyser.byId(chapter.id.value) map { analysis =>
      val pgn = Pgn(
        tags = makeTags(study, chapter)(using flags),
        turns = toTurns(chapter.root)(using flags).toList,
        initial = Initial(
          chapter.root.comments.value.map(_.text.value) ::: shapeComment(chapter.root.shapes).toList
        )
      )
      annotator toPgnString analysis.fold(pgn)(annotator.addEvals(pgn, _))
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
    Tag(_.Annotator, s"https://lichess.org/@/${ownerName(study)}")

  private def makeTags(study: Study, chapter: Chapter)(using flags: WithFlags): Tags =
    Tags {
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
      ) ::: (!chapter.root.fen.isInitial).??(
        List(
          Tag(_.FEN, chapter.root.fen.value),
          Tag("SetUp", "1")
        )
      ) :::
        flags.source.??(List(Tag("Source", chapterUrl(study.id, chapter.id)))) :::
        flags.orientation.??(List(Tag("Orientation", chapter.setup.orientation.name)))
      genTags
        .foldLeft(chapter.tags.value.reverse) { case (tags, tag) =>
          if (tags.exists(t => tag.name == t.name)) tags
          else tag :: tags
        }
        .reverse
    }

object PgnDump:

  case class WithFlags(
      comments: Boolean,
      variations: Boolean,
      clocks: Boolean,
      source: Boolean,
      orientation: Boolean
  )

  private type Variations = Vector[Node]
  private val noVariations: Variations = Vector.empty

  private def node2move(node: Node, variations: Variations)(using flags: WithFlags) =
    chessPgn.Move(
      san = node.move.san,
      glyphs = if (flags.comments) node.glyphs else Glyphs.empty,
      comments = flags.comments ?? {
        node.comments.value.map(_.text.value) ::: shapeComment(node.shapes).toList
      },
      opening = none,
      result = none,
      variations = flags.variations ?? {
        variations.view.map { child =>
          toTurns(child.mainline, noVariations).toList
        }.toList
      },
      secondsLeft = flags.clocks ?? node.clock.map(_.roundSeconds)
    )

  // [%csl Gb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]
  private def shapeComment(shapes: Shapes): Option[String] =
    def render(as: String)(shapes: List[String]) =
      shapes match
        case Nil    => ""
        case shapes => s"[%$as ${shapes.mkString(",")}]"
    val circles = render("csl") {
      shapes.value.collect { case Shape.Circle(brush, orig) =>
        s"${brush.head.toUpper}${orig.key}"
      }
    }
    val arrows = render("cal") {
      shapes.value.collect { case Shape.Arrow(brush, orig, dest) =>
        s"${brush.head.toUpper}${orig.key}${dest.key}"
      }
    }
    s"$circles$arrows".some.filter(_.nonEmpty)

  def toTurn(first: Node, second: Option[Node], variations: Variations)(using WithFlags) =
    chessPgn.Turn(
      number = first.fullMoveNumber.value,
      white = node2move(first, variations).some,
      black = second map { node2move(_, first.children.variations) }
    )

  def toTurns(root: Node.Root)(using WithFlags): Vector[chessPgn.Turn] =
    toTurns(root.mainline, root.children.variations)

  def toTurns(
      line: Vector[Node],
      variations: Variations
  )(using WithFlags): Vector[chessPgn.Turn] = {
    line match
      case Vector() => Vector()
      case first +: rest if first.ply.isEven =>
        chessPgn.Turn(
          number = 1 + (first.ply.value - 1) / 2,
          white = none,
          black = node2move(first, variations).some
        ) +: toTurnsFromWhite(rest, first.children.variations)
      case l => toTurnsFromWhite(l, variations)
  }.filterNot(_.isEmpty)

  def toTurnsFromWhite(line: Vector[Node], variations: Variations)(using WithFlags): Vector[chessPgn.Turn] =
    line
      .grouped(2)
      .foldLeft(variations -> Vector.empty[chessPgn.Turn]) { case ((variations, turns), pair) =>
        pair.headOption.fold(variations -> turns) { first =>
          pair
            .lift(1)
            .getOrElse(first)
            .children
            .variations -> (toTurn(first, pair lift 1, variations) +: turns)
        }
      }
      ._2
      .reverse
