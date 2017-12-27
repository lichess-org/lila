package lila.study

import chess.format.FEN
import chess.format.pgn.{ Parser, ParsedPgn, Tag }
import lila.common.LightUser
import lila.game.{ Game, Namer }
import lila.tree.Node.Comment
import lila.user.User

private final class ExplorerGame(
    importer: lila.explorer.ExplorerImporter,
    lightUser: LightUser.GetterSync,
    baseUrl: String
) {

  def quote(gameId: Game.ID): Fu[Option[Comment]] =
    importer(gameId) map {
      _ ?? { game =>
        gameComment(game).some
      }
    }

  private def truncateFen(f: FEN) = f.value split ' ' take 4 mkString " "

  private def compareFens(a: FEN, b: FEN) = truncateFen(a) == truncateFen(b)

  def insert(userId: User.ID, study: Study, position: Position, gameId: Game.ID): Fu[Option[(Chapter, Path)]] =
    importer(gameId) map {
      _ ?? { game =>
        position.node ?? { fromNode =>
          GameToRoot(game, none, false).|> { root =>
            root.setCommentAt(
              comment = gameComment(game),
              path = Path(root.mainline.map(_.id))
            )
          } ?? { gameRoot =>
            val truncated = truncateFen(fromNode.fen)
            gameRoot.mainline.find(n => truncateFen(n.fen) == truncated).pp("gameNode") flatMap { gameNode =>
              position.chapter.mergeNode(gameNode, position.path).pp("mergeNode") map (_ -> (position.path + gameNode))
            }
          }
        }
      }
    }

  private def gameComment(game: Game) = Comment(
    id = Comment.Id.make,
    text = Comment.Text(s"${gameTitle(game)}, ${gameUrl(game)}"),
    by = Comment.Author.Lichess
  )

  private def gameUrl(game: Game) = s"$baseUrl/${game.id}"

  private def gameTitle(g: Game): String = {
    val pgn = g.pgnImport.flatMap(pgnImport => Parser.full(pgnImport.pgn).toOption)
    val white = pgn.flatMap(_.tags(_.White)) | Namer.playerText(g.whitePlayer)(lightUser)
    val black = pgn.flatMap(_.tags(_.Black)) | Namer.playerText(g.blackPlayer)(lightUser)
    val result = chess.Color.showResult(g.winnerColor)
    val event: Option[String] =
      (pgn.flatMap(_.tags(_.Event)), pgn.flatMap(_.tags.year).map(_.toString)) match {
        case (Some(event), Some(year)) if event.contains(year) => event.some
        case (Some(event), Some(year)) => s"$event, $year".some
        case (eventO, yearO) => eventO orElse yearO
      }
    s"$white - $black, $result, ${event | "-"}"
  }
}
