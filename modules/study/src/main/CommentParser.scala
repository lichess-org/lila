package lila.study

import scalalib.StringOps.softCleanUp
import chess.format.pgn.Comment as CommentStr
import chess.{ Centis, Square }

import lila.tree.Node.{ Comment as TreeComment, Shape, Shapes }

private object CommentParser:

  private val annoRegex =
    """(?s)\[\%anno[\s\r\n]++(?:"([^"]*+)"|([^\],]++))[\s\r\n]*+(?:,[\s\r\n]*+([^\]\s]++))?[\s\r\n]*+\]""".r.unanchored
  private val circlesRegex = """(?s)\[\%csl[\s\r\n]++((?:\w{3}[,\s]*+)++)\]""".r.unanchored
  private val arrowsRegex = """(?s)\[\%cal[\s\r\n]++((?:\w{5}[,\s]*+)++)\]""".r.unanchored
  private val tcecClockRemoveRegex = """tl=[\d:\.]++""".r

  case class Author(name: String, accountId: Option[UserId])

  case class ParsedComment(
      shapes: Shapes,
      clock: Option[Centis],
      emt: Option[Centis],
      comment: CommentStr
  )

  def apply(comment: CommentStr): ParsedComment =
    import TreeComment.*
    ParsedComment(
      parseShapes(comment),
      clk(comment).orElse(tcec(comment)),
      emt(comment),
      rewriteChessBasePieces:
        removeMeta(comment.map(tcecClockRemoveRegex.replaceAllIn(_, ""))).map(_.trim)
    )

  def author(comment: CommentStr): Option[Author] =
    comment.value match
      case annoRegex(quoted, bare, id) =>
        Option(quoted)
          .orElse(Option(bare))
          .map(_.trim)
          .filter(_.nonEmpty)
          .map(name => Author(name, Option(id).flatMap(UserStr.read).map(_.id)))
      case _ => none

  def sanitize(text: String) =
    rewriteChessBasePieces(CommentStr(text.take(4000))).map: text =>
      softCleanUp(text)
        .replaceAll("""\r\n""", "\n") // these 3 lines dedup white spaces and new lines
        .replaceAll("""(?m)(^ *| +(?= |$))""", "")
        .replaceAll("""(?m)^$([\n]+?)(^$[\n]+?^)+""", "$1")
        .replaceAll("[{}]", "") // {} are reserved in PGN comments

  // ChessBase writes piece icons as U+E024..U+E029 (king, queen, rook, bishop, knight, pawn).
  // Rewrite those private-use characters as Lichess piece glyphs.
  private def rewriteChessBasePieces(c: CommentStr): CommentStr = c.map: text =>
    if text.exists(c => c >= '\ue024' && c <= '\ue029') then
      val sb = java.lang.StringBuilder(text.length)
      text.foreach: c =>
        sb.append(c match
          case '\ue024' => '\u2654'
          case '\ue025' => '\u2655'
          case '\ue026' => '\u2656'
          case '\ue027' => '\u2657'
          case '\ue028' => '\u2658'
          case '\ue029' => '\u2659'
          case other => other)
      sb.toString
    else text

  private def parseShapes(comment: CommentStr): Shapes =
    parseCircles(comment) ++ parseArrows(comment)

  private def parseCircles(comment: CommentStr): Shapes =
    comment.value match
      case circlesRegex(str) =>
        val circles = str.split(',').toList.map(_.trim).flatMap { c =>
          for
            color <- c.headOption
            pos <- Square.fromKey(c.drop(1))
          yield Shape.Circle(toBrush(color), pos)
        }
        Shapes(circles)
      case _ => Shapes(Nil)

  private def parseArrows(comment: CommentStr): Shapes =
    comment.value match
      case arrowsRegex(str) =>
        val arrows = str.split(',').toList.map(_.trim).flatMap { c =>
          for
            color <- c.headOption
            orig <- Square.fromKey(c.slice(1, 3))
            dest <- Square.fromKey(c.slice(3, 5))
          yield Shape.Arrow(toBrush(color), orig, dest)
        }
        Shapes(arrows)
      case _ => Shapes(Nil)

  private def toBrush(color: Char): Shape.Brush =
    color match
      case 'G' => "green"
      case 'R' => "red"
      case 'Y' => "yellow"
      case _ => "blue"
