package lila.study

import lila.game.{ Game, Namer, PgnImport }
import lila.user.User
import chess.format.pgn.{ Parser, Reader, ParsedPgn, Tag, TagType }

private final class ExplorerGame(
    importer: lila.explorer.ExplorerImport
) {

  def quote(userId: User.ID, study: Study, chapter: Chapter, path: Path, gameId: Game.ID): Fu[Comment] =
    importer(gameId) flatMap {
      _.fold(false) { game =>

          val comment = Comment(
            id = Comment.Id.make,
            text = game.pgnImport.fold(lichessTitle)(importTitle(game)),
            by = Comment.Author.User(author.id, author.titleName)
          )
      }
    }

  private def importTitle(g: Game)(pgnImport: PgnImport): String =
    Parser.full(pgnImport.pgn) flatMap {
    case ParsedPgn(_, tags, _) => 
        def tag(which: Tag.type => TagType): Option[String] =
          tags find (_.name == which(Tag)) map (_.value)

    ImportData(pgnImport.pgn, none).preprocess(none).fold(
      _ => lichessTitle(g),
      processed =>
        val players = Namer.vsText(game, withRatings = true)
        val result = chess.Color.showResult(game.winnerColor)
        val text = s"$players, $result
  }

  def insert(userId: User.ID, study: Study, chapter: Chapter, gameId: Game.ID) = ???
}
