package lila.study

import chess.format.Forsyth
import chess.format.pgn.{ Pgn, Tag, TagType, Parser, ParsedPgn }
import chess.format.{ pgn => chessPgn }
import org.joda.time.format.DateTimeFormat

import lila.common.LightUser
import lila.game.{ Game, GameRepo }

final class PgnDump(
    chapterRepo: ChapterRepo,
    gamePgnDump: lila.game.PgnDump,
    lightUser: LightUser.Getter,
    netBaseUrl: String) {

  def apply(study: Study): Fu[List[Pgn]] =
    chapterRepo.orderedByStudy(study.id) flatMap { chapters =>
      chapters.map { apply(study, _) }.sequenceFu
    }

  def apply(study: Study, chapter: Chapter): Fu[Pgn] =
    (chapter.setup.gameId ?? GameRepo.gameWithInitialFen) map {
      case Some((game, initialFen)) => gamePgnDump(game, initialFen.map(_.value))
      case None => Pgn(
        tags(study, chapter),
        toTurns(chapter.root, chapter.root.mainLine))
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
      Tag(_.Event, s"${study.name} | ${chapter.name}"),
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

  private def node2move(parent: RootOrNode, node: Node) = chessPgn.Move(
    san = node.move.san,
    glyphs = node.glyphs,
    comments = node.comments.list.map(_.text.value),
    opening = none,
    result = none,
    variations = parent.children.variations.toList.map { child =>
      toTurns(node, child.mainLine)
    })

  private def toTurn(parent: RootOrNode, first: Node, second: Option[Node]) = chessPgn.Turn(
    number = first.fullMoveNumber,
    white = node2move(parent, first).some,
    black = second map { node2move(first, _) })

  private def toTurns(parent: RootOrNode, line: List[Node]): List[chessPgn.Turn] = (line match {
    case Nil => Nil
    case first :: rest if first.ply % 2 == 0 => chessPgn.Turn(
      number = 1 + (first.ply - 1) / 2,
      white = none,
      black = node2move(parent, first).some
    ) :: toTurnsFromWhite(first, rest)
    case l => toTurnsFromWhite(parent, l)
  }) filterNot (_.isEmpty)

  private def toTurnsFromWhite(parent: RootOrNode, line: List[Node]): List[chessPgn.Turn] =
    (line grouped 2).toList.foldLeft(parent -> List.empty[chessPgn.Turn]) {
      case ((parent, turns), pair) => pair.headOption.fold(parent -> turns) { first =>
        pair.lift(1).getOrElse(first) -> (toTurn(parent, first, pair lift 1) :: turns)
      }
    }._2.reverse
}
