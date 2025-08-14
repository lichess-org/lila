package lila.study

import chess.format.pgn.Comment as ChessComment
import chess.{ Centis, Square }

import lila.tree.Node.{ Comment as TreeComment, Shape, Shapes }

private[study] object CommentParser:

  private val circlesRegex = """(?s)\[\%csl[\s\r\n]++((?:\w{3}[,\s]*+)++)\]""".r.unanchored
  private val arrowsRegex = """(?s)\[\%cal[\s\r\n]++((?:\w{5}[,\s]*+)++)\]""".r.unanchored
  private val tcecClockRemoveRegex = """tl=[\d:\.]++""".r

  case class ParsedComment(
      shapes: Shapes,
      clock: Option[Centis],
      emt: Option[Centis],
      comment: String
  )

  def apply(comment: ChessComment): ParsedComment =
    import TreeComment.*
    val text = comment.value
    ParsedComment(
      parseShapes(text),
      clk(text).orElse(tcec(text)),
      emt(text),
      removeMeta(tcecClockRemoveRegex.replaceAllIn(text, "")).trim
    )

  private def parseShapes(comment: String): Shapes =
    parseCircles(comment) ++ parseArrows(comment)

  private def parseCircles(comment: String): Shapes =
    comment match
      case circlesRegex(str) =>
        val circles = str.split(',').toList.map(_.trim).flatMap { c =>
          for
            color <- c.headOption
            pos <- Square.fromKey(c.drop(1))
          yield Shape.Circle(toBrush(color), pos)
        }
        Shapes(circles)
      case _ => Shapes(Nil)

  private def parseArrows(comment: String): Shapes =
    comment match
      case arrowsRegex(str) =>
        val arrows = str.split(',').toList.flatMap { c =>
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
