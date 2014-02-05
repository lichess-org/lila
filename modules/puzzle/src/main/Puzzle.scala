package lila.puzzle

import chess.Color
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
    depth: Int,
    color: Color,
    date: DateTime,
    rating: Glicko,
    vote: Int,
    attempts: Int,
    wins: Int,
    time: Int) {

  def winPercent = if (attempts == 0) 0 else wins * 100 / attempts

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
    depth = (Line minPlyDepth lines) / 2,
    color = Color(history.size % 2 == 0),
    date = DateTime.now,
    rating = Glicko.default,
    vote = 0,
    attempts = 0,
    wins = 0,
    time = 0)

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  private implicit val lineBSONHandler = new BSONHandler[BSONDocument, Lines] {
    private def readMove(move: String) = chess.Pos.doublePiotrToKey(move take 2) match {
      case Some(m) ⇒ s"$m${move drop 2}"
      case _       ⇒ sys error s"Invalid piotr move notation: $move"
    }
    def read(doc: BSONDocument): Lines = doc.elements.toList map {
      case (move, BSONBoolean(true))  ⇒ Win(readMove(move))
      case (move, BSONBoolean(false)) ⇒ Retry(readMove(move))
      case (move, more: BSONDocument) ⇒ Node(readMove(move), read(more))
      case (move, value)              ⇒ throw new Exception(s"Can't read value of $move: $value")
    }
    private def writeMove(move: String) = chess.Pos.doubleKeyToPiotr(move take 4) match {
      case Some(m) ⇒ s"$m${move drop 4}"
      case _       ⇒ sys error s"Invalid move notation: $move"
    }
    def write(lines: Lines): BSONDocument = BSONDocument(lines map {
      case Win(move)         ⇒ writeMove(move) -> BSONBoolean(true)
      case Retry(move)       ⇒ writeMove(move) -> BSONBoolean(false)
      case Node(move, lines) ⇒ writeMove(move) -> write(lines)
    })
  }

  object BSONFields {
    val id = "_id"
    val gameId = "gameId"
    val tags = "tags"
    val history = "history"
    val fen = "fen"
    val lines = "lines"
    val depth = "depth"
    val white = "white"
    val date = "date"
    val rating = "rating"
    val vote = "vote"
    val attempts = "attempts"
    val wins = "wins"
    val time = "time"
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
      depth = r int depth,
      color = Color(r bool white),
      date = r date date,
      rating = r.get[Glicko](rating),
      vote = r intD vote,
      attempts = r intD attempts,
      wins = r intD wins,
      time = r intD time)

    def writes(w: BSON.Writer, o: Puzzle) = BSONDocument(
      id -> o.id,
      gameId -> o.gameId,
      tags -> o.tags,
      history -> o.history.mkString(" "),
      fen -> o.fen,
      lines -> o.lines,
      depth -> o.depth,
      white -> o.color.white,
      date -> o.date,
      rating -> o.rating,
      vote -> o.vote,
      attempts -> o.attempts,
      attempts -> o.wins,
      attempts -> o.time)
  }
}
