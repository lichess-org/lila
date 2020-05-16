package lila.importer

import chess.format.{ FEN, Forsyth, Uci, UciCharPair }
import chess.format.pgn.{ Dumper, San }

import lila.game.{ Game, GameRepo }
import lila.tree.{ Branch, Node, Root }

final class Importer(gameRepo: GameRepo)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(data: ImportData, user: Option[String], forceId: Option[String] = None): Fu[Game] = {

    def gameExists(processing: => Fu[Game]): Fu[Game] =
      gameRepo.findPgnImport(data.pgn) flatMap { _.fold(processing)(fuccess) }

    gameExists {
      (data preprocess user).future flatMap {
        case Preprocessed(g, _, initialFen, _) =>
          val game = forceId.fold(g.sloppy)(g.withId)
          (gameRepo.insertDenormalized(game, initialFen = initialFen)) >> {
            game.pgnImport.flatMap(_.user).isDefined ?? gameRepo.setImportCreatedAt(game)
          } >> {
            gameRepo.finish(
              id = game.id,
              winnerColor = game.winnerColor,
              winnerId = None,
              status = game.status
            )
          } inject game
      }
    }
  }

  def inMemory(data: ImportData): Valid[(Game, Option[FEN], Root)] =
    data.preprocess(user = none).map {
      case preprocessed @ Preprocessed(game, _, fen, _) =>
        (game withId "synthetic", fen, makeRoot(preprocessed))
    }

  private def makeRoot(preprocessed: Preprocessed): Root =
    preprocessed match {
      case Preprocessed(game, replay, initialFen, parsed) =>
        val variations = makeVariations(replay.setup, parsed.sans.value)
        Root(
          ply = replay.setup.turns,
          fen = (initialFen | FEN(game.variant.initialFen)).value,
          check = replay.setup.situation.check,
          clock = parsed.tags.clockConfig.map(_.limit),
          crazyData = replay.setup.situation.board.crazyData,
          children = makeNode(replay.setup, parsed.sans.value).fold(variations)(_ :: variations)
        )
    }

  private def makeNode(prev: chess.Game, sans: List[San]): Option[Branch] =
    sans match {
      case Nil => none
      case san :: rest =>
        san(prev.situation).fold(
          _ => none,
          moveOrDrop => {
            val game       = moveOrDrop.fold(prev.apply, prev.applyDrop)
            val uci        = moveOrDrop.fold(_.toUci, _.toUci)
            val sanStr     = moveOrDrop.fold(Dumper.apply, Dumper.apply)
            val variations = makeVariations(game, rest)
            Branch(
              id = UciCharPair(uci),
              ply = game.turns,
              move = Uci.WithSan(uci, sanStr),
              fen = Forsyth >> game,
              check = game.situation.check,
              comments = makeComments(san.metas.comments),
              glyphs = san.metas.glyphs,
              crazyData = game.situation.board.crazyData,
              children = makeNode(game, rest).fold(variations)(_ :: variations)
            ).some
          }
        )
    }

  private def makeComments(comments: List[String]) =
    Node.Comments {
      comments.map(text =>
        Node.Comment(
          Node.Comment.Id.make,
          Node.Comment.Text(text),
          Node.Comment.Author.Lichess
        )
      )
    }

  private def makeVariations(game: chess.Game, sans: List[San]) =
    sans.headOption ?? {
      _.metas.variations.flatMap(variation => makeNode(game, variation.value))
    }
}
