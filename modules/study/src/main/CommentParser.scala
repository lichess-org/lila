package lila.study

import chess.{ Pos, Color }
import lila.socket.tree.Node.{ Shape, Shapes }

private[study] object CommentParser {

  private val circlesRegex = """(?s).*\[\%csl[\s\r\n]+((?:\w{3}[,\s]*)+)\].*""".r
  private val circlesRemoveRegex = """\[\%csl[\s\r\n]+((?:\w{3}[,\s]*)+)\]""".r
  private val arrowsRegex = """(?s).*\[\%cal[\s\r\n]+((?:\w{5}[,\s]*)+)\].*""".r
  private val arrowsRemoveRegex = """\[\%cal[\s\r\n]+((?:\w{5}[,\s]*)+)\]""".r

  private type ShapesAndComment = (Shapes, String)

  def extractShapes(comment: String): ShapesAndComment =
    parseCircles(comment) match {
      case (circles, c2) => parseArrows(c2) match {
        case (arrows, c3) => (circles ++ arrows) -> c3
      }
    }

  private val clkRemoveRegex = """\[\%clk[\s\r\n]+[\d:]+\]""".r
  def removeClk(comment: String) = clkRemoveRegex.replaceAllIn(comment, "").trim

  private def parseCircles(comment: String): ShapesAndComment = comment match {
    case circlesRegex(str) =>
      val circles = str.split(',').toList.map(_.trim).flatMap { c =>
        for {
          color <- c.headOption
          pos <- Pos posAt c.drop(1)
        } yield Shape.Circle(toBrush(color), pos)
      }
      Shapes(circles) -> circlesRemoveRegex.replaceAllIn(comment, "").trim
    case _ => Shapes(Nil) -> comment
  }

  private def parseArrows(comment: String): ShapesAndComment = comment match {
    case arrowsRegex(str) =>
      val arrows = str.split(',').toList.flatMap { c =>
        for {
          color <- c.headOption
          orig <- Pos posAt c.drop(1).take(2)
          dest <- Pos posAt c.drop(3).take(2)
        } yield Shape.Arrow(toBrush(color), orig, dest)
      }
      Shapes(arrows) -> arrowsRemoveRegex.replaceAllIn(comment, "").trim
    case _ => Shapes(Nil) -> comment
  }

  private def toBrush(color: Char): Shape.Brush = color match {
    case 'G' => "green"
    case 'R' => "red"
    case 'Y' => "yellow"
    case _   => "blue"
  }
}
