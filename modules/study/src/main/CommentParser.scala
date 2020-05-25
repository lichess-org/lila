package lidraughts.study

import draughts.Centis
import draughts.Pos
import lidraughts.tree.Node.{ Shape, Shapes }

private[study] object CommentParser {

  private val circlesRegex = """(?s)\[\%csl[\s\r\n]+((?:\w{3}[,\s]*)+)\]""".r.unanchored
  private val circlesRemoveRegex = """\[\%csl[\s\r\n]+((?:\w{3}[,\s]*)+)\]""".r
  private val arrowsRegex = """(?s)\[\%cal[\s\r\n]+((?:\w{5}[,\s]*)+)\]""".r.unanchored
  private val arrowsRemoveRegex = """\[\%cal[\s\r\n]+((?:\w{5}[,\s]*)+)\]""".r
  private val clockRegex = """(?s)\[\%clock[\s\r\n]+([wW])([\d:\.]+)[\s\r\n]+([bB])([\d:\.]+)\]""".r.unanchored
  private val clockRemoveRegex = """\[\%clock[\s\r\n]+[wW][\d:\.]+[\s\r\n]+[bB][\d:\.]+\]""".r
  private val pgnClockRegex = """(?s)\[\%clk[\s\r\n]+([\d:\.]+)\]""".r.unanchored
  private val pgnClockRemoveRegex = """\[\%clk[\s\r\n]+[\d:\.]+\]""".r
  private val tcecClockRegex = """(?s)tl=([\d:\.]+)""".r.unanchored
  private val tcecClockRemoveRegex = """tl=[\d:\.]+""".r

  case class ParsedComment(
      shapes: Shapes,
      clock: Option[Centis],
      comment: String
  )

  def apply(comment: String, sideToMove: Option[draughts.Color]): ParsedComment =
    parseShapes(comment) match {
      case (shapes, c2) => parseClock(c2, sideToMove: Option[draughts.Color]) match {
        case (clock, c3) => ParsedComment(shapes, clock, c3)
      }
    }

  private type ClockAndComment = (Option[Centis], String)

  private def readCentis(hours: String, minutes: String, seconds: String): Option[Centis] = for {
    h <- parseIntOption(hours)
    m <- parseIntOption(minutes)
    s <- parseIntOption(seconds)
  } yield Centis(h * 360000 + m * 6000 + s * 100)

  private val clockHourMinuteRegex = """^(\d++):(\d+)$""".r
  private val clockHourMinuteSecondRegex = """^(\d++):(\d++)[:\.](\d+)$""".r

  def readCentis(str: String): Option[Centis] = str match {
    case clockHourMinuteRegex(hours, minutes) => readCentis(hours, minutes, "0")
    case clockHourMinuteSecondRegex(hours, minutes, seconds) => readCentis(hours, minutes, seconds)
    case _ => none
  }

  private def parseClock(comment: String, sideToMove: Option[draughts.Color]): ClockAndComment = comment match {
    case clockRegex("w", strW, "B", _) => readCentis(strW) -> clockRemoveRegex.replaceAllIn(comment, "").trim
    case clockRegex("W", _, "b", strB) => readCentis(strB) -> clockRemoveRegex.replaceAllIn(comment, "").trim
    case clockRegex("w", strW, "b", strB) => sideToMove match {
      case Some(draughts.Black) => readCentis(strW) -> clockRemoveRegex.replaceAllIn(comment, "").trim
      case Some(draughts.White) => readCentis(strB) -> clockRemoveRegex.replaceAllIn(comment, "").trim
      case _ => None -> comment
    }
    case pgnClockRegex(str) => readCentis(str) -> pgnClockRemoveRegex.replaceAllIn(comment, "").trim
    case tcecClockRegex(str) => readCentis(str) -> tcecClockRemoveRegex.replaceAllIn(comment, "").trim
    case _ => None -> comment
  }

  def parseRunningClock(comment: String, sideToMove: Option[draughts.Color]): Option[Centis] = comment match {
    case clockRegex("w", _, "B", strB) => readCentis(strB)
    case clockRegex("W", strW, "b", _) => readCentis(strW)
    case clockRegex("w", strW, "b", strB) => sideToMove match {
      case Some(draughts.Black) => readCentis(strB)
      case Some(draughts.White) => readCentis(strW)
      case _ => none
    }
    case _ => none
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
          pos <- draughts.Board.BoardSize.max posAt c.drop(1)
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
          orig <- draughts.Board.BoardSize.max posAt c.drop(1).take(2)
          dest <- draughts.Board.BoardSize.max posAt c.drop(3).take(2)
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
