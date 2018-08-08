package lidraughts.study

import draughts.format.Forsyth
import draughts.format.pdn.{ Pdn, Tag, Tags, Initial }
import draughts.format.{ pdn => draughtsPdn }
import org.joda.time.format.DateTimeFormat

import lidraughts.common.LightUser
import lidraughts.common.String.slugify
import lidraughts.tree.Node.{ Shape, Shapes, Comment }

final class PdnDump(
    chapterRepo: ChapterRepo,
    gamePdnDump: lidraughts.game.PdnDump,
    lightUser: LightUser.GetterSync,
    netBaseUrl: String
) {

  import PdnDump._

  def apply(study: Study): Fu[List[Pdn]] =
    chapterRepo.orderedByStudy(study.id).map {
      _.map { ofChapter(study, _) }
    }

  def ofChapter(study: Study, chapter: Chapter) = Pdn(
    tags = makeTags(study, chapter),
    turns = toTurns(chapter.root),
    initial = Initial(chapter.root.comments.list.map(_.text.value))
  )

  private val fileR = """[\s,]""".r

  def ownerName(study: Study) = lightUser(study.ownerId).fold(study.ownerId)(_.name)

  def filename(study: Study): String = {
    val date = dateFormat.print(study.createdAt)
    fileR.replaceAllIn(
      s"lidraughts_study_${slugify(study.name.value)}_by_${ownerName(study)}_${date}.pdn", ""
    )
  }

  def filename(study: Study, chapter: Chapter): String = {
    val date = dateFormat.print(chapter.createdAt)
    fileR.replaceAllIn(
      s"lidraughts_study_${slugify(study.name.value)}_${slugify(chapter.name.value)}_by_${ownerName(study)}_${date}.pdn", ""
    )
  }

  private def studyUrl(id: Study.Id) = s"$netBaseUrl/study/$id"

  private val dateFormat = DateTimeFormat forPattern "yyyy.MM.dd";

  private def annotatorTag(study: Study) =
    Tag(_.Annotator, s"https://lidraughts.org/@/${ownerName(study)}")

  private def makeTags(study: Study, chapter: Chapter): Tags = Tags {
    val opening = chapter.opening
    val genTags = List(
      Tag(_.Event, s"${study.name}: ${chapter.name}"),
      Tag(_.Site, studyUrl(study.id)),
      Tag(_.UTCDate, Tag.UTCDate.format.print(chapter.createdAt)),
      Tag(_.UTCTime, Tag.UTCTime.format.print(chapter.createdAt)),
      Tag(_.GameType, chapter.setup.variant.gameType),
      Tag(_.ECO, opening.fold("?")(_.eco)),
      Tag(_.Opening, opening.fold("?")(_.name)),
      Tag(_.Result, "*") // required for SCID to import
    ) ::: List(annotatorTag(study)) ::: (chapter.root.fen.value != Forsyth.initial).??(List(
        Tag(_.FEN, chapter.root.fen.value)
      //Tag("SetUp", "1")
      ))
    genTags.foldLeft(chapter.tags.value.reverse) {
      case (tags, tag) =>
        if (tags.exists(t => tag.name == t.name)) tags
        else tag :: tags
    }.reverse
  }
}

private[study] object PdnDump {

  private type Variations = Vector[Node]
  private val noVariations: Variations = Vector.empty

  def node2move(node: Node, variations: Variations) = draughtsPdn.Move(
    san = node.move.san,
    glyphs = node.glyphs,
    comments = node.comments.list.map(_.text.value) ::: shapeComment(node.shapes).toList,
    opening = none,
    result = none,
    variations = variations.map { child =>
      toTurns(child.mainline, noVariations)
    }(scala.collection.breakOut),
    secondsLeft = node.clock.map(_.roundSeconds)
  )

  // [%csl Gb4,Yd5,Rf6][%cal Ge2e4,Ye2d4,Re2g4]
  private def shapeComment(shapes: Shapes): Option[String] = {
    def render(as: String)(shapes: List[String]) = shapes match {
      case Nil => ""
      case shapes => s"[%$as ${shapes.mkString(",")}]"
    }
    val circles = render("csl") {
      shapes.value.collect {
        case Shape.Circle(brush, orig) => s"${brush.head.toUpper}$orig"
      }
    }
    val arrows = render("cal") {
      shapes.value.collect {
        case Shape.Arrow(brush, orig, dest) => s"${brush.head.toUpper}$orig$dest"
      }
    }
    s"$circles$arrows".some.filter(_.nonEmpty)
  }

  def toTurn(first: Node, second: Option[Node], variations: Variations) = draughtsPdn.Turn(
    number = first.fullMoveNumber,
    white = node2move(first, variations).some,
    black = second map { node2move(_, first.children.variations) }
  )

  def toTurns(root: Node.Root): List[draughtsPdn.Turn] = toTurns(root.mainline, root.children.variations)

  def toTurns(line: List[Node], variations: Variations): List[draughtsPdn.Turn] = (line match {
    case Nil => Nil
    case first :: rest if first.ply % 2 == 0 => draughtsPdn.Turn(
      number = 1 + (first.ply - 1) / 2,
      white = none,
      black = node2move(first, variations).some
    ) :: toTurnsFromWhite(rest, first.children.variations)
    case l => toTurnsFromWhite(l, variations)
  }) filterNot (_.isEmpty)

  def toTurnsFromWhite(line: List[Node], variations: Variations): List[draughtsPdn.Turn] =
    (line grouped 2).foldLeft(variations -> List.empty[draughtsPdn.Turn]) {
      case ((variations, turns), pair) => pair.headOption.fold(variations -> turns) { first =>
        pair.lift(1).getOrElse(first).children.variations -> (toTurn(first, pair lift 1, variations) :: turns)
      }
    }._2.reverse
}
