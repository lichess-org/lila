package lila.study

import chess.Pos
import chess.Centis
import lila.tree.Node.{ Shape, Shapes }

private[study] object CommentParser {

  private val circlesRegex = """(?s).*\[\%csl[\s\r\n]+((?:\w{3}[,\s]*)+)\].*""".r
  private val circlesRemoveRegex = """\[\%csl[\s\r\n]+((?:\w{3}[,\s]*)+)\]""".r
  private val arrowsRegex = """(?s).*\[\%cal[\s\r\n]+((?:\w{5}[,\s]*)+)\].*""".r
  private val arrowsRemoveRegex = """\[\%cal[\s\r\n]+((?:\w{5}[,\s]*)+)\]""".r
  private val clockRegex = """(?s).*\[\%clk[\s\r\n]+([\d:]+)\].*""".r
  private val clockRemoveRegex = """\[\%clk[\s\r\n]+[\d:]+\]""".r

  case class ParsedComment(
    shapes: Shapes,
    clock: Option[Centis],
    comment: String
  )

  def apply(comment: String): ParsedComment =
    parseShapes(comment) match {
      case (shapes, c2) => parseClock(c2) match {
        case (clock, c3) => ParsedComment(shapes, clock, c3)
      }
    }

  private type ClockAndComment = (Option[Centis], String)

  private def readCentis(hours: String, minutes: String, seconds: String) = for {
    h <- parseIntOption(hours)
    m <- parseIntOption(minutes)
    s <- parseIntOption(seconds)
  } yield Centis(h * 360000 + m * 6000 + s * 100)

  private def parseClock(comment: String): ClockAndComment = comment match {
    case clockRegex(str) => (str.split(':') match {
      case Array(minutes, seconds) => readCentis("0", minutes, seconds)
      case Array(hours, minutes, seconds) => readCentis(hours, minutes, seconds)
      case _ => none
    }) -> clockRemoveRegex.replaceAllIn(comment, "").trim
    case _ => None -> comment
  }

  private type ShapesAndComment = (Shapes, String)

  private def parseShapes(comment: String): ShapesAndComment =
    parseCircles(comment) match {
      case (circles, comment) => parseArrows(comment) match {
        case (arrows, comment) => (circles ++ arrows) -> comment
      }
    }

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
    case _ => "blue"
  }
}
