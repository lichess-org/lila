package lila.study

import scala.util.chaining._

import chess.format.FEN
import chess.format.pgn.Parser
import lila.game.{ Game, Namer }
import lila.tree.Node.Comment

final private class ExplorerGame(
    importer: lila.explorer.ExplorerImporter,
    lightUserApi: lila.user.LightUserApi,
    net: lila.common.config.NetConfig
)(implicit ec: scala.concurrent.ExecutionContext) {

  def quote(gameId: Game.ID): Fu[Option[Comment]] =
    importer(gameId) map {
      _ ?? { game =>
        gameComment(game).some
      }
    }

  def insert(study: Study, position: Position, gameId: Game.ID): Fu[Option[(Chapter, Path)]] =
    if (position.chapter.isOverweight) {
      logger.info(s"Overweight chapter ${study.id}/${position.chapter.id}")
      fuccess(none)
    } else
      importer(gameId) map {
        _ ?? { game =>
          position.node ?? { fromNode =>
            GameToRoot(game, none, withClocks = false).pipe { root =>
              root.setCommentAt(
                comment = gameComment(game),
                path = Path(root.mainline.map(_.id))
              )
            } ?? { gameRoot =>
              merge(fromNode, position.path, gameRoot) flatMap {
                case (newNode, path) => position.chapter.addNode(newNode, path) map (_ -> path)
              }
            }
          }
        }
      }

  private def truncateFen(f: FEN)         = f.value split ' ' take 4 mkString " "
  private def compareFens(a: FEN, b: FEN) = truncateFen(a) == truncateFen(b)

  private def merge(fromNode: RootOrNode, fromPath: Path, game: Node.Root): Option[(Node, Path)] = {
    val gameNodes = game.mainline.dropWhile(n => !compareFens(n.fen, fromNode.fen)) drop 1
    val (path, foundGameNode) = gameNodes.foldLeft((Path.root, none[Node])) {
      case ((path, None), gameNode) =>
        val nextPath = path + gameNode
        if (fromNode.children.nodeAt(nextPath).isDefined) (nextPath, none)
        else (path, gameNode.some)
      case (found, _) => found
    }
    foundGameNode.map { _ -> fromPath.+(path) }
  }

  private def gameComment(game: Game) =
    Comment(
      id = Comment.Id.make,
      text = Comment.Text(s"${gameTitle(game)}, ${gameUrl(game)}"),
      by = Comment.Author.Lichess
    )

  private def gameUrl(game: Game) = s"${net.baseUrl}/${game.id}"

  private def gameTitle(g: Game): String = {
    val pgn    = g.pgnImport.flatMap(pgnImport => Parser.full(pgnImport.pgn).toOption)
    val white  = pgn.flatMap(_.tags(_.White)) | Namer.playerTextBlocking(g.whitePlayer)(lightUserApi.sync)
    val black  = pgn.flatMap(_.tags(_.Black)) | Namer.playerTextBlocking(g.blackPlayer)(lightUserApi.sync)
    val result = chess.Color.showResult(g.winnerColor)
    val event: Option[String] =
      (pgn.flatMap(_.tags(_.Event)), pgn.flatMap(_.tags.year).map(_.toString)) match {
        case (Some(event), Some(year)) if event.contains(year) => event.some
        case (Some(event), Some(year))                         => s"$event, $year".some
        case (eventO, yearO)                                   => eventO orElse yearO
      }
    s"$white - $black, $result, ${event | "-"}"
  }
}
