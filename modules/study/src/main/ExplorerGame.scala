package lila.study

import chess.format.{ Fen, UciPath }
import chess.format.pgn.{ Parser, Tags, Comment as CommentStr }

import lila.tree.Node.Comment
import lila.tree.{ Branch, Node, Root }

final private class ExplorerGameApi(
    explorer: lila.core.game.Explorer,
    namer: lila.core.game.Namer,
    lightUserApi: lila.core.user.LightUserApi,
    net: lila.core.config.NetConfig
)(using Executor):

  def quote(gameId: GameId): Fu[Option[Comment]] =
    explorer(gameId).map2(gameComment)

  def insert(study: Study, position: Position, gameId: GameId): Fu[Option[(Chapter, UciPath)]] =
    if position.chapter.isOverweight then
      logger.error(s"Overweight chapter ${study.id}/${position.chapter.id}")
      fuccess(none)
    else
      explorer(gameId).mapz: game =>
        position.node.so: fromNode =>
          GameToRoot(game, none, withClocks = false)
            .pipe: root =>
              root.setCommentAt(
                comment = gameComment(game),
                path = UciPath.fromIds(root.mainline.map(_.id))
              )
            .so: gameRoot =>
              merge(fromNode, position.path, gameRoot).flatMap { (newNode, path) =>
                position.chapter.addNode(newNode, path).map(_ -> path)
              }

  private def compareFens(a: Fen.Full, b: Fen.Full, strict: Boolean) =
    if strict then a == b else a.simple == b.simple

  private def gameNodes(fromNode: Node, game: Root, firstTry: Boolean): List[Branch] =
    if compareFens(fromNode.fen, game.fen, firstTry) then game.mainline
    else
      val nodes = game.mainline.dropWhile(n => !compareFens(n.fen, fromNode.fen, firstTry))
      if nodes.nonEmpty || !firstTry then nodes.drop(1) else gameNodes(fromNode, game, false)

  private def merge(fromNode: Node, fromPath: UciPath, game: Root): Option[(Branch, UciPath)] =
    val (path, foundGameNode) = gameNodes(fromNode, game, true).foldLeft((UciPath.root, none[Branch])):
      case ((path, None), gameNode) =>
        val nextPath = path + gameNode.id
        if fromNode.children.nodeAt(nextPath).isDefined then (nextPath, none)
        else (path, gameNode.some)
      case (found, _) => found
    foundGameNode.map { _ -> fromPath.+(path) }

  private def gameComment(game: Game) =
    Comment(
      id = Comment.Id.make,
      text = CommentStr(s"${gameTitle(game)}, ${gameUrl(game)}"),
      by = Comment.Author.Lichess
    )

  private def gameUrl(game: Game) = s"${net.baseUrl}/${game.id}"

  private def gameTitle(g: Game): String =
    val tags = g.pgnImport.flatMap(pgni => Parser.tags(pgni.pgn).toOption).getOrElse(Tags.empty)
    gameTitle(g, tags)

  private def gameTitle(g: Game, tags: Tags): String =
    val white = tags(_.White) | namer.playerTextBlocking(g.whitePlayer)(using lightUserApi.sync)
    val black = tags(_.Black) | namer.playerTextBlocking(g.blackPlayer)(using lightUserApi.sync)
    val result = chess.Outcome.showResult(chess.Outcome(g.winnerColor).some)
    val event: Option[String] =
      (tags(_.Event), tags.year.map(_.toString)) match
        case (Some(event), Some(year)) if event.contains(year) => event.some
        case (Some(event), Some(year)) => s"$event, $year".some
        case (eventO, yearO) => eventO.orElse(yearO)
    s"$white - $black, $result, ${event | "-"}"
