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
      comment: ChessComment
  )

  def apply(comment: ChessComment): ParsedComment =
    import TreeComment.*
    ParsedComment(
      parseShapes(comment),
      clk(comment).orElse(tcec(comment)),
      emt(comment),
      removeMeta(comment.map(tcecClockRemoveRegex.replaceAllIn(_, ""))).map(_.trim)
    )

  private def parseShapes(comment: ChessComment): Shapes =
    parseCircles(comment) ++ parseArrows(comment)

  private def parseCircles(comment: ChessComment): Shapes =
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

  private def parseArrows(comment: ChessComment): Shapes =
    comment.value match
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
