package lila.study

import org.joda.time.DateTime
import scala.util.Try

import chess.format.pgn.{ Parser, Reader, ParsedPgn, Tag, TagType }
import lila.common.LightUser
import lila.game.{ Game, Namer }
import lila.tree.Node.Comment
import lila.user.User

private final class ExplorerGame(
    importer: lila.explorer.ExplorerImporter,
    lightUser: LightUser.GetterSync
) {

  def quote(userId: User.ID, gameId: Game.ID): Fu[Option[Comment]] =
    lightUser(userId) ?? { author =>
      importer(gameId) map {
        _ ?? { game =>
          Comment(
            id = Comment.Id.make,
            text = Comment.Text(gameTitle(game)),
            by = Comment.Author.User(author.id, author.titleName)
          ).some
        }
      }
    }

  private def gameYear(pgn: Option[ParsedPgn], g: Game): Int = pgn.flatMap { p =>
    p.tag(_.UTCDate) orElse p.tag(_.Date)
  }.flatMap { pgnDate =>
    Try(DateTime.parse(pgnDate, Tag.UTCDate.format)).toOption map (_.getYear)
  } | g.createdAt.getYear

  private def gameTitle(g: Game): String = {
    val pgn = g.pgnImport.flatMap(pgnImport => Parser.full(pgnImport.pgn).toOption)
    val white = pgn.flatMap(_.tag(_.White)) | Namer.playerText(g.whitePlayer)(lightUser)
    val black = pgn.flatMap(_.tag(_.Black)) | Namer.playerText(g.blackPlayer)(lightUser)
    val result = chess.Color.showResult(g.winnerColor)
    val event = {
      val raw = pgn.flatMap(_.tag(_.Event))
      val year = gameYear(pgn, g)
      if (raw.exists(_ contains year)) raw
      else raw.fold(year.toString)(e => s"$e, $year")
    }
    s"$white - $black, $result, $event"
  }

  def insert(userId: User.ID, study: Study, chapter: Chapter, gameId: Game.ID) = ???
}
