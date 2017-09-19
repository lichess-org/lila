package lila.study

import org.joda.time.DateTime
import scala.util.Try

import chess.format.pgn.{ Parser, Reader, ParsedPgn, Tag, TagType }
import lila.common.LightUser
import lila.game.{ Game, Namer }
import lila.round.JsonView.WithFlags
import lila.tree.Node.Comment
import lila.user.User

private final class ExplorerGame(
    importer: lila.explorer.ExplorerImporter,
    lightUser: LightUser.GetterSync
) {

  def quote(gameId: Game.ID): Fu[Option[Comment]] =
    importer(gameId) map {
      _ ?? { game =>
        gameComment(game).some
      }
    }

  def insert(userId: User.ID, study: Study, position: Position, gameId: Game.ID): Fu[Option[(Chapter, Path)]] =
    importer(gameId) map {
      _ ?? { game =>
        position.node ?? { fromNode =>
          val gameRoot = GameToRoot(game, none, false)
          merge(fromNode, position.path, gameRoot, gameComment(game)) flatMap {
            case (newNode, path) => position.chapter.addNode(newNode, path) map (_ -> path)
          }
        }
      }
    }

  private def merge(fromNode: RootOrNode, fromPath: Path, game: Node.Root, comment: Comment): Option[(Node, Path)] = {
    val gameNodes = game.mainline.take(lila.explorer.maxPlies).reverse
    val fromNodes = fromNode.mainline.take(lila.explorer.maxPlies - fromNode.ply)
    val fromEndPath = fromNodes.drop(1).foldLeft(fromPath)(_ + _)
    val (path, foundGameNode) = fromNodes.reverse.foldLeft((fromEndPath, none[Node])) {
      case ((path, None), fromNode) =>
        path.init -> gameNodes.find(_.fen == fromNode.fen)
      case (found, _) => found
    }
    foundGameNode.flatMap(_.children.first).map { nextGameNode =>
      val commentedNode = nextGameNode setComment comment
      commentedNode -> path
    }
  }

  private def gameComment(game: Game) = Comment(
    id = Comment.Id.make,
    text = Comment.Text(gameTitle(game)),
    by = Comment.Author.Lichess
  )

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
}
