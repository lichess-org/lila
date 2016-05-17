package lila.study

import scalaz.Validation.FlatMap._

import chess.format.pgn.{ Pgn, Tag, TagType, Parser, ParsedPgn, Glyphs, San, Dumper }
import chess.format.{ Forsyth, FEN, Uci, UciCharPair }
import lila.importer.{ ImportData, Preprocessed }
import lila.socket.tree.Node.{ Comment, Comments }

private object PgnImport {

  case class Result(
    root: Node.Root,
    variant: chess.variant.Variant,
    tags: List[Tag])

  def apply(pgn: String): Valid[Result] =
    ImportData(pgn, analyse = none).preprocess(user = none).map {
      case Preprocessed(game, replay, result, initialFen, parsedPgn) => Result(
        root = Node.Root(
          ply = replay.setup.turns,
          fen = initialFen | FEN(game.variant.initialFen),
          check = replay.setup.situation.check,
          shapes = Nil,
          comments = Comments(Nil),
          glyphs = Glyphs.empty,
          crazyData = replay.setup.situation.board.crazyData,
          children = makeNode(
            prev = replay.setup,
            sans = parsedPgn.sans,
            annotator = parsedPgn.tag("annotator").map(Comment.Author.External.apply)
          ).fold(
              err => {
                logger.warn(s"PgnImport $err")
                Node.emptyChildren
              },
              node => Node.Children(node.toVector)
            )),
        variant = game.variant,
        tags = parsedPgn.tags filter isRelevant)
    }

  private def isRelevant(tag: Tag) = relevantTags(tag.name) && !unknownValues(tag.value)

  private val unknownValues = Set("", "?", "unknown")

  private val relevantTags: Set[chess.format.pgn.TagType] = {
    import Tag._
    Set(Event, Site, Date, Round, White, Black, TimeControl,
      WhiteElo, BlackElo, WhiteTitle, BlackTitle,
      Tag.Result, Tag.FEN, Variant, Termination, Annotator)
  }

  private def makeNode(prev: chess.Game, sans: List[San], annotator: Option[Comment.Author]): Valid[Option[Node]] =
    sans match {
      case Nil => success(none)
      case san :: rest => san(prev.situation) flatMap { moveOrDrop =>
        val game = moveOrDrop.fold(prev.apply, prev.applyDrop)
        val uci = moveOrDrop.fold(_.toUci, _.toUci)
        val sanStr = moveOrDrop.fold(Dumper.apply, Dumper.apply)
        makeNode(game, rest, annotator) map { mainline =>
          val variations = rest.headOption.?? {
            _.metas.variations.flatMap { variation =>
              makeNode(game, variation, annotator).fold({ err =>
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
            comments = Comments(san.metas.comments map { text =>
              Comment(Comment.Id.make, Comment.Text(text), annotator | Comment.Author.Lichess)
            }),
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
