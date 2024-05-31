package lila.study

import akka.stream.scaladsl._
import shogi.{ Piece, Pos, Role }
import shogi.format.forsyth.Sfen
import shogi.format.kif.Kif
import shogi.format.csa.Csa
import shogi.format.{ Glyphs, Initial, NotationStep, Tag, Tags }
import shogi.variant.Variant
import org.joda.time.format.DateTimeFormat

import lila.common.String.slugify
import lila.tree.Node.{ Comment, Comments, Shape, Shapes }

final class NotationDump(
    chapterRepo: ChapterRepo,
    lightUserApi: lila.user.LightUserApi,
    net: lila.common.config.NetConfig
) {

  import NotationDump._

  def apply(study: Study, flags: WithFlags): Source[String, _] =
    chapterRepo
      .orderedByStudySource(study.id)
      .map(ofChapter(study, flags))
      .map(_.toString)
      .intersperse("\n\n\n")

  def ofChapter(study: Study, flags: WithFlags)(chapter: Chapter) = {
    val variant = chapter.setup.variant
    val tags    = makeTags(study, chapter)
    val moves   = toMoves(chapter.root, variant)(flags).toList
    val initial = Initial(
      renderComments(chapter.root.comments, chapter.root.hasMultipleCommentAuthors) ::: shapeComment(
        chapter.root.shapes
      ).toList
    )
    if (flags.csa && variant.standard) Csa(moves, chapter.root.sfen.some, initial, tags)
    else Kif(moves, chapter.root.sfen.some, variant, initial, tags)
  }

  private val fileR = """[\s,]""".r

  def ownerName(study: Study) = lightUserApi.sync(study.ownerId).fold(study.ownerId)(_.name)

  def filename(study: Study): String = {
    val date = dateFormat.print(study.createdAt)
    java.net.URLEncoder.encode(
      fileR.replaceAllIn(
        s"lishogi_study_${slugify(study.name.value)}_by_${ownerName(study)}_${date}",
        ""
      ),
      "UTF-8"
    )
  }

  def filename(study: Study, chapter: Chapter): String = {
    val date = dateFormat.print(chapter.createdAt)
    java.net.URLEncoder.encode(
      fileR.replaceAllIn(
        s"lishogi_study_${slugify(study.name.value)}_${slugify(chapter.name.value)}_by_${ownerName(study)}_${date}",
        ""
      ),
      "UTF-8"
    )
  }

  private def chapterUrl(studyId: Study.Id, chapterId: Chapter.Id) =
    s"${net.baseUrl}/study/$studyId/$chapterId"

  private val dateFormat = DateTimeFormat forPattern "yyyy.MM.dd"

  private def makeTags(study: Study, chapter: Chapter): Tags =
    Tags {
      List(
        Tag(_.Event, s"${study.name} - ${chapter.name}"),
        Tag(_.Site, chapterUrl(study.id, chapter.id)),
        Tag(_.Annotator, ownerName(study))
      ).foldLeft(chapter.tags.value.reverse) { case (tags, tag) =>
        if (tags.exists(t => tag.name == t.name)) tags
        else tag :: tags
      }.reverse
    }
}

object NotationDump {

  case class WithFlags(
      csa: Boolean,
      comments: Boolean,
      variations: Boolean,
      shiftJis: Boolean,
      clocks: Boolean
  )

  private type Variations = Vector[Node]
  private val noVariations: Variations = Vector.empty

  private def shapeComment(shapes: Shapes): Option[String] = {
    def renderBrush(brush: String): Char =
      brush match {
        case "alternative0" | "red"    => 'R'
        case "alternative1" | "blue"   => 'B'
        case "alternative2" | "yellow" => 'Y'
        case _                         => 'G'
      }
    def render(as: String)(shapes: List[String]) =
      shapes match {
        case Nil    => ""
        case shapes => s"[%$as ${shapes.mkString(",")}]"
      }
    def pieceLetter(p: Piece): String = {
      val roleStr = Role.allDroppable
        .find(_ == p.role)
        .flatMap(r => shogi.format.usi.Usi.Drop.roleToUsi.get(r))
        .getOrElse(p.role.name.head.toString)
      if (p.color.sente) roleStr.toUpperCase else roleStr
    }

    def writePosOrPiece(pop: Either[Pos, Piece]): String =
      pop.fold(_.key, p => ("_" + pieceLetter(p)) takeRight 2)

    val circles = render("csl") {
      shapes.value.collect { case Shape.Circle(brush, orig, None) =>
        s"${renderBrush(brush)}${writePosOrPiece(orig)}"
      }
    }
    val pieces = render("cpl") {
      shapes.value.collect { case Shape.Circle(brush, Left(orig), Some(piece)) =>
        s"${renderBrush(brush)}${orig.key}${pieceLetter(piece)}"
      }
    }
    val arrows = render("cal") {
      shapes.value.collect { case Shape.Arrow(brush, orig, dest) =>
        s"${renderBrush(brush)}${writePosOrPiece(orig)}${writePosOrPiece(dest)}"
      }
    }
    s"$circles$arrows$pieces".some.filter(_.nonEmpty)
  }

  private def renderComments(comments: Comments, showAuthors: Boolean): List[String] = {
    def getName(author: Comment.Author) = {
      val nameOpt = author match {
        case Comment.Author.User(_, id)  => id.some
        case Comment.Author.External(id) => id.some
        case Comment.Author.Lishogi      => "lishogi".some
        case Comment.Author.Unknown      => none
      }
      nameOpt.filter(_.nonEmpty).map(n => s"[${n}] ").getOrElse("")
    }
    comments.list.map(c => s"${showAuthors ?? s"${getName(c.by)}"}${c.text.value}")
  }

  def toMoves(root: Node.Root, variant: Variant)(implicit flags: WithFlags): Vector[NotationStep] =
    toMoves(
      root.mainline,
      root.sfen,
      variant,
      root.children.variations,
      root.hasMultipleCommentAuthors
    )

  def toMoves(
      line: Vector[Node],
      initialSfen: Sfen,
      variant: Variant,
      variations: Variations,
      showAuthors: Boolean
  )(implicit flags: WithFlags): Vector[NotationStep] = {
    val enriched = shogi.Replay.usiWithRoleWhilePossible(line.map(_.usi), initialSfen.some, variant)
    line
      .zip(enriched)
      .foldLeft(Vector.empty[NotationStep]) { case (moves, (node, usiWithRole)) =>
        NotationStep(
          stepNumber = node.ply,
          usiWithRole = usiWithRole,
          glyphs = if (flags.comments) node.glyphs else Glyphs.empty,
          comments = flags.comments ?? {
            renderComments(node.comments, showAuthors) ::: shapeComment(node.shapes).toList
          },
          result = none,
          variations = flags.variations ?? {
            val parentNode = line.lift(moves.size - 1)
            (parentNode
              .fold(variations)(_.children.variations))
              .view
              .map { child =>
                toMoves(
                  child.mainline,
                  parentNode.fold(initialSfen)(_.sfen),
                  variant,
                  noVariations,
                  showAuthors
                ).toList
              }
              .toList
          }
        ) +: moves
      }
      .reverse
  }

}
