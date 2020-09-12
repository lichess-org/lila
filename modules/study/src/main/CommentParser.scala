package lila.study

import chess.Centis
import chess.Pos
import lila.common.Maths
import lila.tree.Node.{ Shape, Shapes }

private[study] object CommentParser {

  private val circlesRegex         = """(?s)\[\%csl[\s\r\n]+((?:\w{3}[,\s]*)+)\]""".r.unanchored
  private val circlesRemoveRegex   = """\[\%csl[\s\r\n]+((?:\w{3}[,\s]*)+)\]""".r
  private val arrowsRegex          = """(?s)\[\%cal[\s\r\n]+((?:\w{5}[,\s]*)+)\]""".r.unanchored
  private val arrowsRemoveRegex    = """\[\%cal[\s\r\n]+((?:\w{5}[,\s]*)+)\]""".r
  private val clockRegex           = """(?s)\[\%clk[\s\r\n]+([\d:\.]+)\]""".r.unanchored
  private val clockRemoveRegex     = """\[\%clk[\s\r\n]+[\d:\.]+\]""".r
  private val tcecClockRegex       = """(?s)tl=([\d:\.]+)""".r.unanchored
  private val tcecClockRemoveRegex = """tl=[\d:\.]+""".r

  case class ParsedComment(
      shapes: Shapes,
      clock: Option[Centis],
      comment: String
  )

  def apply(comment: String): ParsedComment =
    parseShapes(comment) match {
      case (shapes, c2) =>
        parseClock(c2) match {
          case (clock, c3) => ParsedComment(shapes, clock, c3)
        }
    }

  private type ClockAndComment = (Option[Centis], String)

  private def readCentis(hours: String, minutes: String, seconds: String): Option[Centis] =
    for {
      h <- hours.toIntOption
      m <- minutes.toIntOption
      cs <- seconds.toDoubleOption match {
        case Some(s) => Some(Maths.roundAt(s * 100, 0).toInt)
        case _       => none
      }
    } yield Centis(h * 360000 + m * 6000 + cs)

  private val clockHourMinuteRegex                 = """^(\d++):(\d+)$""".r
  private val clockHourMinuteSecondRegex           = """^(\d++):(\d++)[:\.](\d+)$""".r
  private val clockHourMinuteFractionalSecondRegex = """^(\d++):(\d++):(\d++\.\d+)$""".r

  def readCentis(str: String): Option[Centis] =
    str match {
      case clockHourMinuteRegex(hours, minutes)                => readCentis(hours, minutes, "0")
      case clockHourMinuteSecondRegex(hours, minutes, seconds) => readCentis(hours, minutes, seconds)
      case clockHourMinuteFractionalSecondRegex(hours, minutes, seconds) =>
        readCentis(hours, minutes, seconds)
      case _ => none
    }

  private def parseClock(comment: String): ClockAndComment =
    comment match {
      case clockRegex(str)     => readCentis(str) -> clockRemoveRegex.replaceAllIn(comment, "").trim
      case tcecClockRegex(str) => readCentis(str) -> tcecClockRemoveRegex.replaceAllIn(comment, "").trim
      case _                   => None            -> comment
    }

  private type ShapesAndComment = (Shapes, String)

  private def parseShapes(comment: String): ShapesAndComment =
    parseCircles(comment) match {
      case (circles, comment) =>
        parseArrows(comment) match {
          case (arrows, comment) => (circles ++ arrows) -> comment
        }
    }

  private def parseCircles(comment: String): ShapesAndComment =
    comment match {
      case circlesRegex(str) =>
        val circles = str.split(',').toList.map(_.trim).flatMap { c =>
          for {
            color <- c.headOption
            pos   <- Pos posAt c.drop(1)
          } yield Shape.Circle(toBrush(color), pos)
        }
        Shapes(circles) -> circlesRemoveRegex.replaceAllIn(comment, "").trim
      case _ => Shapes(Nil) -> comment
    }

  private def parseArrows(comment: String): ShapesAndComment =
    comment match {
      case arrowsRegex(str) =>
        val arrows = str.split(',').toList.flatMap { c =>
          for {
            color <- c.headOption
            orig  <- Pos posAt c.slice(1, 3)
            dest  <- Pos posAt c.slice(3, 5)
          } yield Shape.Arrow(toBrush(color), orig, dest)
        }
        Shapes(arrows) -> arrowsRemoveRegex.replaceAllIn(comment, "").trim
      case _ => Shapes(Nil) -> comment
    }

  private def toBrush(color: Char): Shape.Brush =
    color match {
      case 'G' => "green"
      case 'R' => "red"
      case 'Y' => "yellow"
      case _   => "blue"
    }
}
