package lila.puzzle

import org.joda.time.DateTime
import scalaz.NonEmptyList

import lila.rating.Glicko

case class Puzzle(
    id: PuzzleId,
    gameId: Option[String],
    tags: List[String],
    history: List[String],
    fen: String,
    lines: List[Line],
    date: DateTime,
    rating: Glicko,
    vote: Int,
    attempts: Int) {

  def initialMove = history.last
}

object Puzzle {

  def make(
    gameId: Option[String],
    tags: List[String],
    history: List[String],
    fen: String,
    lines: Lines) = new Puzzle(
    id = ornicar.scalalib.Random nextStringUppercase 8,
    gameId = gameId,
    tags = tags,
    history = history,
    fen = fen,
    lines = lines,
    date = DateTime.now,
    rating = Glicko.default,
    vote = 0,
    attempts = 0)

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  private implicit val lineBSONHandler = new BSONHandler[BSONDocument, Lines] {
    def read(doc: BSONDocument): Lines = doc.elements.toList map {
      case (move, BSONString("win"))   ⇒ Win(move)
      case (move, BSONString("retry")) ⇒ Retry(move)
      case (move, more: BSONDocument)  ⇒ Node(move, read(more))
      case (move, value)               ⇒ throw new Exception(s"Can't read value of $move: $value")
    }
    def write(lines: Lines): BSONDocument = BSONDocument(lines map {
      case Win(move)         ⇒ move -> BSONString("win")
      case Retry(move)       ⇒ move -> BSONString("retry")
      case Node(move, lines) ⇒ move -> write(lines)
    })
  }

  object BSONFields {
    val id = "_id"
    val gameId = "gameId"
    val tags = "tags"
    val white = "white"
    val history = "history"
    val fen = "fen"
    val lines = "lines"
    val date = "date"
    val rating = "rating"
    val vote = "vote"
    val attempts = "attempts"
  }

  implicit val puzzleBSONHandler = new BSON[Puzzle] {

    import BSONFields._
    import Glicko.GlickoBSONHandler

    def reads(r: BSON.Reader): Puzzle = Puzzle(
      id = r str id,
      gameId = r strO gameId,
      tags = r.get[List[String]](tags),
      history = r str history split ' ' toList,
      fen = r str fen,
      lines = r.get[Lines](lines),
      date = r date date,
      rating = r.get[Glicko](rating),
      vote = r intD vote,
      attempts = r intD attempts)

    def writes(w: BSON.Writer, o: Puzzle) = BSONDocument(
      id -> o.id,
      gameId -> o.gameId,
      tags -> o.tags,
      history -> o.history.mkString(" "),
      fen -> o.fen,
      lines -> o.lines,
      date -> o.date,
      rating -> o.rating,
      vote -> o.vote,
      attempts -> o.attempts)
  }
}
