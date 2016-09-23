package lila.study

import chess.format.Forsyth
import chess.format.pgn.{ Pgn, Tag, TagType, Parser, ParsedPgn, Initial }
import chess.format.{ pgn => chessPgn }
import org.joda.time.format.DateTimeFormat

import lila.common.LightUser
import lila.game.{ Game, GameRepo }

final class PgnDump(
    chapterRepo: ChapterRepo,
    gamePgnDump: lila.game.PgnDump,
    lightUser: LightUser.Getter,
    netBaseUrl: String) {

  import PgnDump._

  def apply(study: Study): Fu[List[Pgn]] =
    chapterRepo.orderedByStudy(study.id) flatMap { chapters =>
      chapters.map { ofChapter(study, _) }.sequenceFu
    }

  def ofChapter(study: Study, chapter: Chapter): Fu[Pgn] =
    (chapter.setup.gameId ?? GameRepo.gameWithInitialFen).map {
      case Some((game, initialFen)) => gamePgnDump.tags(game, initialFen.map(_.value), none)
      case None                     => tags(study, chapter)
    }.map { tags =>
      Pgn(
        tags = tags,
        turns =toTurns(chapter.root),
        initial = Initial(chapter.root.comments.list.map(_.text.value)))
    }

  private val fileR = """[\s,]""".r

  def filename(study: Study): String = {
    val name = lila.common.String slugify study.name
    val owner = lightUser(study.ownerId).fold(study.ownerId)(_.name)
    val date = dateFormat.print(study.createdAt)
    fileR.replaceAllIn(s"lichess_study_${name}_by_${owner}_${date}.pgn", "")
  }

  private def studyUrl(id: String) = s"$netBaseUrl/study/$id"

  private val dateFormat = DateTimeFormat forPattern "yyyy.MM.dd";

  private def tags(study: Study, chapter: Chapter): List[Tag] = {
    val opening = chapter.opening
    List(
      Tag(_.Event, s"${study.name}: ${chapter.name}"),
      Tag(_.Site, studyUrl(study.id)),
      Tag(_.Date, dateFormat.print(chapter.createdAt)),
      // Tag(_.White, "?"),
      // Tag(_.Black, "?"),
      Tag(_.Variant, chapter.setup.variant.name.capitalize),
      Tag(_.ECO, opening.fold("?")(_.eco)),
      Tag(_.Opening, opening.fold("?")(_.name))
    ) ::: (chapter.root.fen.value != Forsyth.initial).??(List(
        Tag(_.FEN, chapter.root.fen.value),
        Tag("SetUp", "1")
      ))
  }
}

private[study] object PgnDump {

  private type Variations = Vector[Node]
  private val noVariations: Variations = Vector.empty

  def node2move(node: Node, variations: Variations) = chessPgn.Move(
    san = node.move.san,
    glyphs = node.glyphs,
    comments = node.comments.list.map(_.text.value),
    opening = none,
    result = none,
    variations = variations.toList.map { child =>
      toTurns(child.mainline, noVariations)
    })

  def toTurn(first: Node, second: Option[Node], variations: Variations) = chessPgn.Turn(
    number = first.fullMoveNumber,
    white = node2move(first, variations).some,
    black = second map { node2move(_, first.children.variations) })

  def toTurns(root: Node.Root): List[chessPgn.Turn] = toTurns(root.mainline, root.children.variations)

  def toTurns(line: List[Node], variations: Variations): List[chessPgn.Turn] = (line match {
    case Nil => Nil
    case first :: rest if first.ply % 2 == 0 => chessPgn.Turn(
      number = 1 + (first.ply - 1) / 2,
      white = none,
      black = node2move(first, variations).some
    ) :: toTurnsFromWhite(rest, first.children.variations)
    case l => toTurnsFromWhite(l, variations)
  }) filterNot (_.isEmpty)

  def toTurnsFromWhite(line: List[Node], variations: Variations): List[chessPgn.Turn] =
    (line grouped 2).toList.foldLeft(variations -> List.empty[chessPgn.Turn]) {
      case ((variations, turns), pair) => pair.headOption.fold(variations -> turns) { first =>
        pair.lift(1).getOrElse(first).children.variations -> (toTurn(first, pair lift 1, variations) :: turns)
      }
    }._2.reverse
}
