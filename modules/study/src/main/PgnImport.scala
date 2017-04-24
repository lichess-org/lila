package lila.study

import scalaz.Validation.FlatMap._

import chess.format.pgn.{ Tag, Glyphs, San, Dumper }
import chess.format.{ Forsyth, FEN, Uci, UciCharPair }

import chess.Centis
import lila.importer.{ ImportData, Preprocessed }
import lila.tree.Node.{ Comment, Comments, Shapes }

private object PgnImport {

  case class Result(
    root: Node.Root,
    variant: chess.variant.Variant,
    tags: List[Tag]
  )

  def apply(pgn: String): Valid[Result] =
    ImportData(pgn, analyse = none).preprocess(user = none).map {
      case prep @ Preprocessed(game, replay, result, initialFen, parsedPgn) =>
        val annotator = parsedPgn.tag("annotator").map(Comment.Author.External.apply)
        parseComments(parsedPgn.initialPosition.comments, annotator) match {
          case (shapes, _, comments) =>
            val root = Node.Root(
              ply = replay.setup.turns,
              fen = initialFen | FEN(game.variant.initialFen),
              check = replay.setup.situation.check,
              shapes = shapes,
              comments = comments,
              glyphs = Glyphs.empty,
              clock = parsedPgn.clockConfig.map(_.limit),
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
                node =>
                  Node.Children {
                    val variations = makeVariations(parsedPgn.sans, replay.setup, annotator)
                    node.fold(variations)(_ :: variations).toVector
                  }
              )
            )
            val commented =
              if (root.mainline.lastOption.??(_.isCommented)) root
              else endComment(prep).fold(root) { comment =>
                root updateMainlineLast { _.setComment(comment) }
              }
            Result(
              root = commented,
              variant = game.variant,
              tags = PgnTags(parsedPgn.tags)
            )
        }
    }

  private def endComment(prep: Preprocessed) = {
    import lila.tree.Node.Comment
    import prep._
    val winner = game.winnerColor orElse result.flatMap(_.winner)
    val resultText = chess.Color.showResult(winner)
    (if (game.finished) game.status.some else result.map(_.status)) map { status =>
      val statusText = lila.game.StatusText(status, winner, game.variant)
      val text = s"$resultText $statusText"
      Comment(Comment.Id.make, Comment.Text(text), Comment.Author.Lichess)
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

  private def parseComments(comments: List[String], annotator: Option[Comment.Author]): (Shapes, Option[Centis], Comments) =
    comments.foldLeft(Shapes(Nil), none[Centis], Comments(Nil)) {
      case ((shapes, clock, comments), txt) => CommentParser(txt) match {
        case CommentParser.ParsedComment(s, c, str) => (
          (shapes ++ s),
          c orElse clock,
          (str.trim match {
            case "" => comments
            case com => comments + Comment(Comment.Id.make, Comment.Text(com), annotator | Comment.Author.Lichess)
          })
        )
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
          parseComments(san.metas.comments, annotator) match {
            case (shapes, clock, comments) => Node(
              id = UciCharPair(uci),
              ply = game.turns,
              move = Uci.WithSan(uci, sanStr),
              fen = FEN(Forsyth >> game),
              check = game.situation.check,
              shapes = shapes,
              comments = comments,
              glyphs = san.metas.glyphs,
              crazyData = game.situation.board.crazyData,
              clock = clock,
              children = removeDuplicatedChildrenFirstNode {
                Node.Children {
                  mainline.fold(variations)(_ :: variations).toVector
                }
              }
            ).some
          }
        }
      }
    }

  /*
   * Fix bad PGN like this one found on reddit:
   * 7. c4 (7. c4 Nf6) (7. c4 dxc4) 7... cxd4
   * where 7. c4 appears three times
   */
  private def removeDuplicatedChildrenFirstNode(children: Node.Children): Node.Children = children.first match {
    case Some(main) if children.variations.exists(_.id == main.id) => Node.Children {
      main +: children.variations.flatMap { node =>
        if (node.id == main.id) node.children.nodes
        else Vector(node)
      }
    }
    case _ => children
  }
}
