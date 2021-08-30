package lila.study

import shogi.format.pgn.{ Dumper, Glyphs, ParsedPgn, San, Tags }
import shogi.format.{ FEN, Forsyth, Uci, UciCharPair }
import shogi.Replay

import shogi.Centis
import lila.common.LightUser
import lila.importer.{ ImportData, Preprocessed }
import lila.tree.Node.{ Comment, Comments, Shapes }

import lila.game.Game

object PgnImport {

  case class Result(
      root: Node.Root,
      variant: shogi.variant.Variant,
      tags: Tags,
      end: Option[End]
  )

  case class End(
      status: shogi.Status,
      winner: Option[shogi.Color],
      resultText: String,
      statusText: String
  )

  def apply(pgn: String, contributors: List[LightUser]): Valid[Result] =
    ImportData(pgn, analyse = none).preprocess(user = none).map {
      case Preprocessed(game, replay, initialFen, parsedPgn) =>
        val annotator = findAnnotator(parsedPgn, contributors)
        parseComments(parsedPgn.initialPosition.comments, annotator) match {
          case (shapes, comments) =>
            val root = Node.Root(
              ply = replay.setup.turns,
              fen = initialFen | FEN(game.variant.initialFen),
              check = replay.setup.situation.check,
              shapes = shapes,
              comments = comments,
              glyphs = Glyphs.empty,
              clock = parsedPgn.tags.clockConfig.map(_.limit),
              crazyData = replay.setup.situation.board.crazyData,
              children = Node.Children {
                val variations = makeVariations(parsedPgn.sans.value, replay.setup, annotator)
                makeNode(
                  prev = replay.setup,
                  sans = parsedPgn.sans.value,
                  annotator = annotator
                ).fold(variations)(_ :: variations).toVector
              }
            )
            val end: Option[End] = (game.finished option game.status).map { status =>
              End(
                status = status,
                winner = game.winnerColor,
                resultText = shogi.Color.showResult(game.winnerColor),
                statusText = lila.game.StatusText(status, game.winnerColor, game.variant)
              )
            }
            val commented =
              if (root.mainline.lastOption.??(_.isCommented)) root
              else
                end.map(endComment).fold(root) { comment =>
                  root updateMainlineLast { _.setComment(comment) }
                }
            Result(
              root = commented,
              variant = game.variant,
              tags = PgnTags(parsedPgn.tags),
              end = end
            )
        }
    }

  def userAnalysis(kif: String): Valid[(Game, Option[FEN], Node.Root, Tags)] =
    ImportData(kif, analyse = none).preprocess(user = none).map {
      case Preprocessed(game, replay, initialFen, parsedPgn) =>
        val root = Node.Root(
          ply = replay.setup.turns,
          fen = initialFen | FEN(game.variant.initialFen),
          check = replay.setup.situation.check,
          glyphs = Glyphs.empty,
          clock = parsedPgn.tags.clockConfig.map(_.limit),
          crazyData = replay.setup.situation.board.crazyData,
          children = Node.Children {
            val variations = makeVariations(parsedPgn.sans.value, replay.setup, None)
            makeNode(
              prev = replay.setup,
              sans = parsedPgn.sans.value,
              annotator = None
            ).fold(variations)(_ :: variations).toVector
          }
        )
        (game withId "synthetic", initialFen, root, parsedPgn.tags)
    }

  private def findAnnotator(pgn: ParsedPgn, contributors: List[LightUser]): Option[Comment.Author] =
    pgn tags "annotator" map { a =>
      val lowered = a.toLowerCase
      contributors.find { c =>
        c.name == lowered || c.titleName == lowered || lowered.endsWith(s"/${c.id}")
      } map { c =>
        Comment.Author.User(c.id, c.titleName)
      } getOrElse Comment.Author.External(a)
    }

  private def endComment(end: End): Comment = {
    import lila.tree.Node.Comment
    import end._
    val text = s"$resultText $statusText"
    Comment(Comment.Id.make, Comment.Text(text), Comment.Author.Lishogi)
  }

  private def makeVariations(sans: List[San], game: shogi.Game, annotator: Option[Comment.Author]) =
    sans.headOption.?? {
      _.metas.variations.flatMap { variation =>
        makeNode(game, variation.value, annotator)
      }
    }

  private def parseComments(
      comments: List[String],
      annotator: Option[Comment.Author]
  ): (Shapes, Comments) =
    comments.foldLeft((Shapes(Nil), Comments(Nil))) { case ((shapes, comments), txt) =>
      CommentParser(txt) match {
        case CommentParser.ParsedComment(s, str) =>
          (
            (shapes ++ s),
            (str.trim match {
              case "" => comments
              case com =>
                comments + Comment(Comment.Id.make, Comment.Text(com), annotator | Comment.Author.Unknown)
            })
          )
      }
    }

  private def makeNode(prev: shogi.Game, sans: List[San], annotator: Option[Comment.Author]): Option[Node] =
    try {
      sans match {
        case Nil => none
        case san :: rest =>
          san(prev.situation).fold(
            _ => none, // illegal move; stop here.
            moveOrDrop => {
              val game   = moveOrDrop.fold(prev.apply, prev.applyDrop)
              val uci    = moveOrDrop.fold(_.toUci, _.toUci)
              val sanStr = moveOrDrop.fold(Dumper.apply, Dumper.apply)
              parseComments(san.metas.comments, annotator) match {
                case (shapes, comments) =>
                  Node(
                    id = UciCharPair(uci),
                    ply = game.turns,
                    move = Uci.WithSan(uci, sanStr),
                    fen = FEN(Forsyth >> game),
                    check = game.situation.check,
                    shapes = shapes,
                    comments = comments,
                    glyphs = san.metas.glyphs,
                    crazyData = game.situation.board.crazyData,
                    // Normally we store time remaining after turn,
                    // which is actually pretty useless with byo...
                    // for imports we are gonna store time spent on this move
                    clock = san.metas.timeSpent,
                    children = removeDuplicatedChildrenFirstNode {
                      val variations = makeVariations(rest, game, annotator)
                      Node.Children {
                        makeNode(game, rest, annotator).fold(variations)(_ :: variations).toVector
                      }
                    },
                    forceVariation = false
                  ).some
              }
            }
          )
      }
    } catch {
      case _: StackOverflowError =>
        logger.warn(s"study KifImport.makeNode StackOverflowError")
        None
    }

  /*
   * Fix bad PGN like this one found on reddit:
   * 7. c4 (7. c4 Nf6) (7. c4 dxc4) 7... cxd4
   * where 7. c4 appears three times
   */
  private def removeDuplicatedChildrenFirstNode(children: Node.Children): Node.Children =
    children.first match {
      case Some(main) if children.variations.exists(_.id == main.id) =>
        Node.Children {
          main +: children.variations.flatMap { node =>
            if (node.id == main.id) node.children.nodes
            else Vector(node)
          }
        }
      case _ => children
    }
}
