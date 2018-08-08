package lidraughts.study

import scalaz.Validation.FlatMap._
import scalaz.Validation.success
import draughts.format.pdn.{ Dumper, Glyphs, ParsedPdn, San, Tags }
import draughts.format.{ FEN, Forsyth, Uci, UciCharPair }
import draughts.Centis
import lidraughts.common.LightUser
import lidraughts.importer.{ ImportData, Preprocessed }
import lidraughts.tree.Node.{ Comment, Comments, Shapes }

object PdnImport {

  case class Result(
      root: Node.Root,
      variant: draughts.variant.Variant,
      tags: Tags,
      end: Option[End]
  )

  case class End(
      status: draughts.Status,
      winner: Option[draughts.Color],
      resultText: String,
      statusText: String
  )

  def apply(pdn: String, contributors: List[LightUser]): Valid[Result] =
    ImportData(pdn, analyse = none).preprocess(user = none).map {
      case prep @ Preprocessed(game, replay, result, initialFen, parsedPdn) =>
        val annotator = findAnnotator(parsedPdn, contributors)
        parseComments(parsedPdn.initialPosition.comments, annotator) match {
          case (shapes, _, comments) =>
            val root = Node.Root(
              ply = replay.setup.turns,
              fen = initialFen | FEN(game.variant.initialFen),
              shapes = shapes,
              comments = comments,
              glyphs = Glyphs.empty,
              clock = parsedPdn.tags.clockConfig.map(_.limit),
              children = Node.Children {
                val variations = makeVariations(parsedPdn.sans.value, replay.setup, annotator, iteratedCapts = true, ambs = none)
                var nodeRes = makeNode(
                  prev = replay.setup,
                  sans = parsedPdn.sans.value,
                  annotator = annotator,
                  iteratedCapts = true,
                  ambs = none
                )
                if (nodeRes._1.isDefined && variations._1.nonEmpty) {
                  var ambVars = 0
                  while (variations._1.exists(n => nodeRes._1.get.id == n.id)) {
                    ambVars = ambVars + 1
                    nodeRes = nodeRes._1.get.copy(id = UciCharPair(nodeRes._1.get.id.a, ambVars)).some -> nodeRes._2
                  }
                }
                nodeRes._1.fold(variations._1)(_ :: variations._1).toVector
              }
            )
            val end: Option[End] = {
              (if (game.finished) game.status else result.status).some
                .filter(draughts.Status.Aborted <=).map { status =>
                  val winner = game.winnerColor orElse result.winner
                  End(
                    status = status,
                    winner = winner,
                    resultText = draughts.Color.showResult(winner),
                    statusText = lidraughts.game.StatusText(status, winner, game.variant)
                  )
                }
            }
            val commented =
              if (root.mainline.lastOption.??(_.isCommented)) root
              else end.map(endComment(prep)).fold(root) { comment =>
                root updateMainlineLast { _.setComment(comment) }
              }
            Result(
              root = commented,
              variant = game.variant,
              tags = PdnTags(parsedPdn.tags),
              end = end
            )
        }
    }

  private def findAnnotator(pdn: ParsedPdn, contributors: List[LightUser]): Option[Comment.Author] =
    pdn tags "annotator" map { a =>
      val lowered = a.toLowerCase
      contributors.find { c =>
        c.name == lowered || c.titleName == lowered || lowered.endsWith(s"/${c.id}")
      } map { c =>
        Comment.Author.User(c.id, c.titleName)
      } getOrElse Comment.Author.External(a)
    }

  private def endComment(prep: Preprocessed)(end: End): Comment = {
    import lidraughts.tree.Node.Comment
    import end._
    val text = s"$resultText $statusText"
    Comment(Comment.Id.make, Comment.Text(text), Comment.Author.Lidraughts)
  }

  private def makeVariations(sans: List[San], game: draughts.DraughtsGame, annotator: Option[Comment.Author], iteratedCapts: Boolean = false, ambs: Option[List[(San, String)]]): (List[Node], Boolean) =
    sans.headOption.fold((List[Node](), false)) {
      san =>
        var ids = List[UciCharPair]()
        var illegal = false
        (san.metas.variations.flatMap { variation =>
          var nodeRes = makeNode(game, variation.value, annotator, iteratedCapts, ambs)
          if (nodeRes._1.isDefined) {
            var ambVars = 0
            while (ids.contains(nodeRes._1.get.id)) {
              ambVars = ambVars + 1
              nodeRes = nodeRes._1.get.copy(id = UciCharPair(nodeRes._1.get.id.a, ambVars)).some -> nodeRes._2
            }
            ids = nodeRes._1.get.id :: ids
          }
          illegal = nodeRes._2
          nodeRes._1
        }, illegal)
    }

  private def parseComments(comments: List[String], annotator: Option[Comment.Author]): (Shapes, Option[Centis], Comments) =
    comments.foldLeft(Shapes(Nil), none[Centis], Comments(Nil)) {
      case ((shapes, clock, comments), txt) => CommentParser(txt) match {
        case CommentParser.ParsedComment(s, c, str) => (
          (shapes ++ s),
          c orElse clock,
          (str.trim match {
            case "" => comments
            case com => comments + Comment(Comment.Id.make, Comment.Text(com), annotator | Comment.Author.Lidraughts)
          })
        )
      }
    }

  private def makeNode(prev: draughts.DraughtsGame, sans: List[San], annotator: Option[Comment.Author], iteratedCapts: Boolean = false, ambs: Option[List[(San, String)]]): (Option[Node], Boolean) = {
    var newAmb = none[(San, String)]
    val res = sans match {
      case Nil => (none, false)
      case san :: rest =>
        san(prev.situation, iteratedCapts, if (ambs.isEmpty) None else ambs.get.collect({ case (ambSan, ambUci) if ambSan == san => ambUci }).some).fold(
          _ =>
            (none, true), // illegal move; stop here unless we can rewind to some notational ambiguity
          move => {
            if (iteratedCapts && move.capture.fold(false)(_.lengthCompare(1) > 0) && move.situationBefore.ambiguitiesMove(move) > 0)
              newAmb = (san -> move.toUci.uci).some
            val game = prev.apply(move)
            val uci = move.toUci
            val sanStr = Dumper.apply(move)
            parseComments(san.metas.comments, annotator) match {
              case (shapes, clock, comments) =>
                var illegal = false
                val variations = makeVariations(rest, game, annotator, iteratedCapts, if (newAmb.isDefined) (newAmb.get :: ambs.getOrElse(Nil)).some else ambs)
                (Node(
                  id = UciCharPair(uci),
                  ply = game.turns,
                  move = Uci.WithSan(uci, sanStr),
                  fen = FEN(Forsyth >> game),
                  shapes = shapes,
                  comments = comments,
                  glyphs = san.metas.glyphs,
                  clock = clock,
                  children = Node.Children {
                    var nodeRes = makeNode(game, rest, annotator, iteratedCapts, if (newAmb.isDefined) (newAmb.get :: ambs.getOrElse(Nil)).some else ambs)
                    if (nodeRes._1.isDefined && variations._1.nonEmpty) {
                      var ambVars = 0
                      while (variations._1.exists(n => nodeRes._1.get.id == n.id)) {
                        ambVars = ambVars + 1
                        nodeRes = nodeRes._1.get.copy(id = UciCharPair(nodeRes._1.get.id.a, ambVars)).some -> nodeRes._2
                      }
                    }
                    illegal = nodeRes._2 || variations._2
                    nodeRes._1.fold(variations._1)(_ :: variations._1).toVector
                  }
                ).some, illegal)
            }
          }
        )
    }
    if (res._2 && newAmb.isDefined) makeNode(prev, sans, annotator, iteratedCapts, (newAmb.get :: ambs.getOrElse(Nil)).some)
    else res
  }

  /*
   * Fix bad PDN like this one found on reddit:
   * 7. c4 (7. c4 Nf6) (7. c4 dxc4) 7... cxd4
   * where 7. c4 appears three times
   *
   * Disabled because this might erroneously remove a notational ambiguity:
   * 1. 48x30 (1. 48x30 14-20 2. 30-25 22-28) 1... 14-20 2. 30-25 21-27
   *
   */
  /*private def removeDuplicatedChildrenFirstNode(children: Node.Children): Node.Children = children.first match {
    case Some(main) if children.variations.exists(_.id == main.id) => Node.Children {
      main +: children.variations.flatMap { node =>
        if (node.id == main.id) node.children.nodes
        else Vector(node)
      }
    }
    case _ => children
  }*/
}
