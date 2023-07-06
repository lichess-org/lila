package lila.study

import scala.util.chaining.*

import chess.format.{ Fen, UciPath }
import chess.format.pgn.Parser
import lila.game.{ Game, Namer }
import lila.tree.{ Branch, Root, Node }
import lila.tree.Node.Comment

final private class ExplorerGame(
    importer: lila.explorer.ExplorerImporter,
    lightUserApi: lila.user.LightUserApi,
    net: lila.common.config.NetConfig
)(using Executor):

  def quote(gameId: GameId): Fu[Option[Comment]] =
    importer(gameId) mapz { game =>
      gameComment(game).some
    }

  def insert(study: Study, position: Position, gameId: GameId): Fu[Option[(Chapter, UciPath)]] =
    if position.chapter.isOverweight then
      logger.info(s"Overweight chapter ${study.id}/${position.chapter.id}")
      fuccess(none)
    else
      importer(gameId) mapz { game =>
        position.node so { fromNode =>
          GameToRoot(game, none, withClocks = false).pipe { root =>
            root.setCommentAt(
              comment = gameComment(game),
              path = UciPath.fromIds(root.mainline.map(_.id))
            )
          } so { gameRoot =>
            merge(fromNode, position.path, gameRoot) flatMap { case (newNode, path) =>
              position.chapter.addNode(newNode, path) map (_ -> path)
            }
          }
        }
      }

  private def compareFens(a: Fen.Epd, b: Fen.Epd) = a.simple == b.simple

  private def merge(fromNode: Node, fromPath: UciPath, game: Root): Option[(Branch, UciPath)] =
    val gameNodes = game.mainline.dropWhile(n => !compareFens(n.fen, fromNode.fen)) drop 1
    val (path, foundGameNode) = gameNodes.foldLeft((UciPath.root, none[Branch])) {
      case ((path, None), gameNode) =>
        val nextPath = path + gameNode.id
        if fromNode.children.nodeAt(nextPath).isDefined then (nextPath, none)
        else (path, gameNode.some)
      case (found, _) => found
    }
    foundGameNode.map { _ -> fromPath.+(path) }

  private def gameComment(game: Game) =
    Comment(
      id = Comment.Id.make,
      text = Comment.Text(s"${gameTitle(game)}, ${gameUrl(game)}"),
      by = Comment.Author.Lichess
    )

  private def gameUrl(game: Game) = s"${net.baseUrl}/${game.id}"

  private def gameTitle(g: Game): String =
    val pgn = g.pgnImport.flatMap(pgnImport => Parser.full(pgnImport.pgn).toOption)
    val white =
      pgn.flatMap(_.tags(_.White)) | Namer.playerTextBlocking(g.whitePlayer)(using lightUserApi.sync)
    val black =
      pgn.flatMap(_.tags(_.Black)) | Namer.playerTextBlocking(g.blackPlayer)(using lightUserApi.sync)
    val result = chess.Outcome.showResult(chess.Outcome(g.winnerColor).some)
    val event: Option[String] =
      (pgn.flatMap(_.tags(_.Event)), pgn.flatMap(_.tags.year).map(_.toString)) match
        case (Some(event), Some(year)) if event.contains(year) => event.some
        case (Some(event), Some(year))                         => s"$event, $year".some
        case (eventO, yearO)                                   => eventO orElse yearO
    s"$white - $black, $result, ${event | "-"}"
