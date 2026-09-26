package lila.study

import chess.format.{ Fen, Uci, UciPath }
import chess.format.pgn.{ Parser, Tags, Comment as CommentStr }
import chess.variant.Variant

import lila.tree.Node.Comment
import lila.tree.{ Branch, Branches, Node, Root }

final private class ExplorerGameApi(
    explorer: lila.core.game.Explorer,
    namer: lila.core.game.Namer,
    lightUserApi: lila.core.user.LightUserApi,
    net: lila.core.config.NetConfig
)(using Executor):

  import ExplorerGameApi.*

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
              val variant = position.chapter.setup.variant
              merge(fromNode, position.path, gameRoot, variant, gameId).flatMap { (newNode, path) =>
                position.chapter.addNode(newNode, path).map(_ -> path)
              }

  private def gameComment(game: Game) =
    Comment(
      id = Comment.Id.make,
      text = CommentStr(s"${gameTitle(game)}, ${gameUrl(game.id)}"),
      by = Comment.Author.Lichess
    )

  private def gameUrl(gameId: GameId) = s"${net.baseUrl}/$gameId"

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

private object ExplorerGameApi:

  def merge(
      fromNode: Node,
      fromPath: UciPath,
      game: Root,
      variant: Variant,
      gameId: GameId
  ): Option[(Branch, UciPath)] =
    @annotation.tailrec
    def dropKnown(parent: Node, path: UciPath, nodes: List[Branch]): (Node, UciPath, List[Branch]) =
      nodes match
        case gameNode :: rest =>
          parent.children.get(gameNode.id) match
            case Some(child) => dropKnown(child, path + gameNode.id, rest)
            case None => (parent, path, nodes)
        case Nil => (parent, path, nodes)
    val (parent, path, newNodes) = dropKnown(fromNode, UciPath.root, gameNodes(fromNode, game))
    replay(parent, newNodes, variant, gameId).map { _ -> fromPath.+(path) }

  private def replay(parent: Node, nodes: List[Branch], variant: Variant, gameId: GameId): Option[Branch] =
    val setup = chess.Position.AndFullMoveNumber(variant, parent.fen)
    val sources = nodes.toVector
    val (result, error) = setup.position.foldRight(nodes.map(_.move.uci), parent.ply)(
      none[Branch],
      (step, acc) =>
        val source = sources((step.ply - parent.ply - 1).value)
        inline def branch = makeBranch(source, step.move, step.ply)
        acc.fold(branch)(branch.prependChildUnchecked).some
    )
    error.foreach: err =>
      logger.warn(s"ExplorerGame replay /$gameId ${err.value}")
    result

  private def gameNodes(fromNode: Node, game: Root): List[Branch] =
    val mainline = game.mainline
    mainline.lastIndexWhere(n => compareFens(n.fen, fromNode.fen, false)) match
      case -1 => if compareFens(game.fen, fromNode.fen, false) then mainline else Nil
      case i => mainline.drop(i + 1)

  private def compareFens(a: Fen.Full, b: Fen.Full, strict: Boolean) =
    if strict then a == b else a.simple == b.simple

  private def makeBranch(source: Branch, move: chess.MoveOrDrop, ply: chess.Ply): Branch =
    source.copy(
      ply = ply,
      move = Uci.WithSan(move.toUci, move.toSanStr),
      fen = Fen.write(move.after, ply.fullMoveNumber),
      crazyData = move.after.crazyData,
      children = Branches.empty
    )
