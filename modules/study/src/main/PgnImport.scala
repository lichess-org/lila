package lila.study

import scalaz.Validation.FlatMap._

import chess.format.pgn.{ Pgn, Tag, TagType, Parser, ParsedPgn, Glyphs, San, Dumper }
import chess.format.{ Forsyth, FEN, Uci, UciCharPair }
import lila.importer.{ ImportData, Preprocessed }
import lila.socket.tree.Node.{ Comment, Comments }

private object PgnImport {

  private type TagPicker = Tag.type => TagType

  case class Result(
    root: Node.Root,
    variant: chess.variant.Variant)

  def apply(pgn: String): Valid[Result] =
    ImportData(pgn, analyse = none).preprocess(user = none).map {
      case Preprocessed(game, replay, result, initialFen, ParsedPgn(tags, sans)) => Result(
        root = Node.Root(
          ply = replay.setup.turns,
          fen = initialFen | FEN(game.variant.initialFen),
          check = replay.setup.situation.check,
          shapes = Nil,
          comments = Comments(Nil),
          glyphs = Glyphs.empty,
          crazyData = replay.setup.situation.board.crazyData,
          children = makeNode(replay.setup, sans).fold(
            err => {
              logger.warn(s"PgnImport $err")
              Node.emptyChildren
            },
            node => Node.Children(node.toVector)
          )),
        variant = game.variant)
    }

  private def makeNode(prev: chess.Game, sans: List[San]): Valid[Option[Node]] =
    sans match {
      case Nil => success(none)
      case san :: rest => san(prev.situation) flatMap { moveOrDrop =>
        val game = moveOrDrop.fold(prev.apply, prev.applyDrop)
        val uci = moveOrDrop.fold(_.toUci, _.toUci)
        val sanStr = moveOrDrop.fold(Dumper.apply, Dumper.apply)
        makeNode(game, rest) map { mainline =>
          val variations = rest.headOption.?? {
            _.metas.variations.flatMap { variation =>
              makeNode(game, variation).fold({ err =>
                logger.warn(s"$variation $err")
                none
              }, identity)
            }
          }
          Node(
            id = UciCharPair(uci),
            ply = game.turns,
            move = Uci.WithSan(uci, sanStr),
            fen = FEN(Forsyth >> game),
            check = game.situation.check,
            shapes = Nil,
            comments = Comments(san.metas.comments map Comment.byUnknown),
            glyphs = san.metas.glyphs,
            crazyData = game.situation.board.crazyData,
            children = Node.Children {
              mainline.fold(variations)(_ :: variations).toVector
            }
          ).some
        }
      }
    }
}
