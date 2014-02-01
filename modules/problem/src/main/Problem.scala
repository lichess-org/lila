package lila.problem

import org.joda.time.DateTime
import scalaz.NonEmptyList

sealed trait Line
case class Node(move: String, lines: Lines) extends Line
case class End(move: String) extends Line

case class Problem(
  id: ProblemId,
  gameId: String,
  tags: List[String],
  white: Boolean,
  position: List[String],
  lines: List[Line],
  date: DateTime)

object Problem {

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  private implicit val lineBSONHandler = new BSONHandler[BSONDocument, Lines] {
    def read(doc: BSONDocument): Lines = doc.elements.toList map {
      case (move, BSONString("end"))  ⇒ End(move)
      case (move, more: BSONDocument) ⇒ Node(move, read(more))
      case (move, value)              ⇒ throw new Exception(s"Can't read value of $move: $value")
    }
    def write(lines: Lines): BSONDocument = BSONDocument(lines map {
      case End(move)         ⇒ move -> BSONString("end")
      case Node(move, lines) ⇒ move -> write(lines)
    })
  }
  implicit val problemBSONHandler = Macros.handler[Problem]
}
