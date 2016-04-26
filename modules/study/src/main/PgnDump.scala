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
    netBaseUrl: String) {

  def apply(study: Study): Fu[List[Pgn]] =
    chapterRepo.orderedByStudy(study.id) flatMap { chapters =>
      chapters.map { chapter =>
        (chapter.setup.gameId ?? GameRepo.gameWithInitialFen) map {
          case Some((game, initialFen)) => gamePgnDump(game, initialFen.map(_.value))
          case None => Pgn(
            tags(study, chapter),
            turns(
              chapter.root.mainLine.map(_.move.san),
              1 + chapter.root.ply / 2))
        }
      }.sequenceFu
    }

  private val fileR = """[\s,]""".r

  def filename(study: Study): String = {
    val name = lila.common.String slugify study.name
    val owner = study.owner.??(_.user.name)
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
      Tag(_.White, "?"),
      Tag(_.Black, "?"),
      Tag(_.Variant, chapter.setup.variant.name.capitalize),
      Tag(_.ECO, opening.fold("?")(_.eco)),
      Tag(_.Opening, opening.fold("?")(_.name))
    ) ::: (chapter.root.fen.value != Forsyth.initial).??(List(
        Tag(_.FEN, chapter.root.fen.value),
        Tag("SetUp", "1")
      ))
  }

  private def turns(moves: List[String], from: Int): List[chessPgn.Turn] =
    (moves grouped 2).zipWithIndex.toList map {
      case (moves, index) => chessPgn.Turn(
        number = index + from,
        white = moves.headOption filter (".." !=) map { chessPgn.Move(_) },
        black = moves lift 1 map { chessPgn.Move(_) })
    } filterNot (_.isEmpty)
}
