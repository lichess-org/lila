package lila.study

import scalaz.Validation.FlatMap._

import chess.format.pgn.{ Pgn, Tag, TagType, Parser, ParsedPgn, Glyphs, San, Dumper }
import chess.format.{ Forsyth, FEN, Uci, UciCharPair }
import lila.importer.{ ImportData, Preprocessed }
import lila.socket.tree.Node.{ Comment, Comments, Shape, Shapes }

private object PgnImport {

  case class Result(
    root: Node.Root,
    variant: chess.variant.Variant,
    tags: List[Tag])

  def apply(pgn: String): Valid[Result] =
    ImportData(pgn, analyse = none).preprocess(user = none).map {
      case Preprocessed(game, replay, result, initialFen, parsedPgn) =>
        val annotator = parsedPgn.tag("annotator").map(Comment.Author.External.apply)
        makeShapesAndComments(parsedPgn.initialPosition.comments, annotator) match {
          case (shapes, comments) => Result(
            root = Node.Root(
              ply = replay.setup.turns,
              fen = initialFen | FEN(game.variant.initialFen),
              check = replay.setup.situation.check,
              shapes = shapes,
              comments = comments,
              glyphs = Glyphs.empty,
              crazyData = replay.setup.situation.board.crazyData,
              children = makeNode(
                prev = replay.setup,
                sans = parsedPgn.sans,
                annotator = annotator
              ).fold(
                  err => {
                    logger.warn(s"PgnImport $err")
                    Node.emptyChildren
                  },
                  node => {
                    Node.Children {
                      val variations = makeVariations(parsedPgn.sans, replay.setup, annotator)
                      node.fold(variations)(_ :: variations).toVector
                    }
                  })),
            variant = game.variant,
            tags = PgnTags(parsedPgn.tags))
        }
    }

  private def makeVariations(sans: List[San], game: chess.Game, annotator: Option[Comment.Author]) =
    sans.headOption.?? {
      _.metas.variations.flatMap { variation =>
        makeNode(game, variation, annotator).fold({ err =>
          logger.warn(s"$variation $err")
          none
        }, identity)
      }
    }

  private def makeShapesAndComments(comments: List[String], annotator: Option[Comment.Author]): (Shapes, Comments) =
    comments.foldLeft(Shapes(Nil), Comments(Nil)) {
      case ((shapes, comments), txt) => ShapeParser(txt) match {
        case (s, c) => (shapes ++ s) -> (c.trim match {
          case ""  => comments
          case com => comments + Comment(Comment.Id.make, Comment.Text(com), annotator | Comment.Author.Lichess)
        })
      }
    }

  private def makeNode(prev: chess.Game, sans: List[San], annotator: Option[Comment.Author]): Valid[Option[Node]] =
    sans match {
      case Nil => success(none)
      case san :: rest => san(prev.situation) flatMap { moveOrDrop =>
        val game = moveOrDrop.fold(prev.apply, prev.applyDrop)
        val uci = moveOrDrop.fold(_.toUci, _.toUci)
        val sanStr = moveOrDrop.fold(Dumper.apply, Dumper.apply)
        makeNode(game, rest, annotator) map { mainline =>
          val variations = makeVariations(rest, game, annotator)
          makeShapesAndComments(san.metas.comments, annotator) match {
            case (shapes, comments) => Node(
              id = UciCharPair(uci),
              ply = game.turns,
              move = Uci.WithSan(uci, sanStr),
              fen = FEN(Forsyth >> game),
              check = game.situation.check,
              shapes = shapes,
              comments = comments,
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
}
