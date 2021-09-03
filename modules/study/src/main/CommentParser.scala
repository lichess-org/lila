package lila.study

import shogi.Centis
import shogi.Pos
import lila.tree.Node.{ Shape, Shapes }

private[study] object CommentParser {

  private val circlesRegex       = """(?s)\[\%csl[\s\r\n]+((?:\w{3}[,\s]*)+)\]""".r.unanchored
  private val circlesRemoveRegex = """\[\%csl[\s\r\n]+((?:\w{3}[,\s]*)+)\]""".r
  private val arrowsRegex        = """(?s)\[\%cal[\s\r\n]+((?:\w{5}[,\s]*)+)\]""".r.unanchored
  private val arrowsRemoveRegex  = """\[\%cal[\s\r\n]+((?:\w{5}[,\s]*)+)\]""".r
  private val piecesRegex        = """(?s)\[\%cpl[\s\r\n]+((?:\w{4}[,\s]*)+)\]""".r.unanchored
  private val piecesRemoveRegex  = """\[\%cpl[\s\r\n]+((?:\w{4}[,\s]*)+)\]""".r

  case class ParsedComment(
      shapes: Shapes,
      comment: String
  )

  def apply(comment: String): ParsedComment =
    parseShapes(comment) match {
      case (shapes, c2) => ParsedComment(shapes, c2)
    }

  private type ShapesAndComment = (Shapes, String)

  private def parseShapes(comment: String): ShapesAndComment =
    parseCircles(comment) match {
      case (circles, comment) =>
        parseArrows(comment) match {
          case (arrows, comment) => 
            parsePieces(comment) match {
              case (pieces, comment) => (circles ++ arrows ++ pieces) -> comment
            }
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
            orig  <- Pos posAt c.drop(1).take(2)
            dest  <- Pos posAt c.drop(3).take(2)
          } yield Shape.Arrow(toBrush(color), orig, dest)
        }
        Shapes(arrows) -> arrowsRemoveRegex.replaceAllIn(comment, "").trim
      case _ => Shapes(Nil) -> comment
    }

  private def parsePieces(comment: String): ShapesAndComment =
    comment match {
      case piecesRegex(str) =>
        val pieces = str.split(',').toList.map(_.trim).flatMap { c =>
          for {
            color <- c.headOption
            pos   <- Pos posAt c.drop(1).take(2)
            pieceChar <- c.lastOption
            piece <- shogi.Piece.fromChar(pieceChar)
          } yield Shape.Piece(toBrush(color), pos, piece)
        }
        Shapes(pieces) -> piecesRemoveRegex.replaceAllIn(comment, "").trim
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
