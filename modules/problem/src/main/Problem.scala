package lila.problem

import chess.Color
import org.joda.time.DateTime
import scalaz.NonEmptyList

sealed trait Line
case class Node(move: String, lines: Lines) extends Line
case class End(move: String) extends Line

case class Problem(
  id: ProblemId,
  hash: String,
  gameId: Option[String],
  tags: List[String],
  color: Color,
  position: List[String],
  lines: List[Line],
  date: DateTime)

object Problem {

  def make(
    gameId: Option[String],
    tags: List[String],
    color: Color,
    position: List[String],
    lines: Lines) = new Problem(
    id = ornicar.scalalib.Random nextStringUppercase 8,
    hash = makeHash(position),
    gameId = gameId,
    tags = tags,
    color = color,
    position = position,
    lines = lines,
    date = DateTime.now)

  def makeHash(position: List[String]) = {
    import com.roundeights.hasher.Implicits._
    position.mkString.md5.toString take 12
  }

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
  implicit val problemBSONHandler = new BSON[Problem] {

    def reads(r: BSON.Reader): Problem = Problem(
      id = r str "_id",
      hash = r str "hash",
      gameId = r strO "gameId",
      tags = r.get[List[String]]("tags"),
      color = Color(r bool "white"),
      position = r str "position" split ' ' toList,
      lines = r.get[Lines]("lines"),
      date = r date "date")

    def writes(w: BSON.Writer, o: Problem) = BSONDocument(
      "_id" -> o.id,
      "hash" -> o.hash,
      "gameId" -> o.gameId,
      "tags" -> o.tags,
      "white" -> o.color.white,
      "position" -> o.position.mkString(" "),
      "lines" -> o.lines,
      "date" -> o.date)
  }
}
