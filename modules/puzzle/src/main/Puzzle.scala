package lila.puzzle

import chess.Color
import org.joda.time.DateTime
import scalaz.NonEmptyList

import lila.rating.Perf

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
    perf: Perf,
    vote: Vote,
    attempts: Int,
    wins: Int,
    time: Int) {

  def initialPly: Option[Int] = fen.split(' ').lastOption flatMap parseIntOption map { move =>
    move * 2 + color.fold(0, 1)
  }

  def withVote(f: Vote => Vote) = copy(vote = f(vote))

  def winPercent = if (attempts == 0) 0 else wins * 100 / attempts

  def initialMove = history.last

  def enabled = vote.sum > -9000
}

object Puzzle {

  def make(
    gameId: Option[String],
    tags: List[String],
    history: List[String],
    fen: String,
    lines: Lines)(id: PuzzleId) = new Puzzle(
    id = id,
    gameId = gameId,
    tags = tags,
    history = history,
    fen = fen,
    lines = lines,
    depth = Line minDepth lines,
    color = Color(history.size % 2 == 0),
    date = DateTime.now,
    perf = Perf.default,
    vote = Vote(0, 0, 0),
    attempts = 0,
    wins = 0,
    time = 0)

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  private implicit val lineBSONHandler = new BSONHandler[BSONDocument, Lines] {
    private def readMove(move: String) = chess.Pos.doublePiotrToKey(move take 2) match {
      case Some(m) => s"$m${move drop 2}"
      case _       => sys error s"Invalid piotr move notation: $move"
    }
    def read(doc: BSONDocument): Lines = doc.elements.toList map {
      case (move, BSONBoolean(true))  => Win(readMove(move))
      case (move, BSONBoolean(false)) => Retry(readMove(move))
      case (move, more: BSONDocument) => Node(readMove(move), read(more))
      case (move, value)              => throw new Exception(s"Can't read value of $move: $value")
    }
    private def writeMove(move: String) = chess.Pos.doubleKeyToPiotr(move take 4) match {
      case Some(m) => s"$m${move drop 4}"
      case _       => sys error s"Invalid move notation: $move"
    }
    def write(lines: Lines): BSONDocument = BSONDocument(lines map {
      case Win(move)         => writeMove(move) -> BSONBoolean(true)
      case Retry(move)       => writeMove(move) -> BSONBoolean(false)
      case Node(move, lines) => writeMove(move) -> write(lines)
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
    val perf = "perf"
    val rating = s"$perf.gl.r"
    val vote = "vote"
    val voteSum = s"$vote.sum"
    val attempts = "attempts"
    val wins = "wins"
    val time = "time"
    val users = "users"
  }

  val withoutUsers = BSONDocument(BSONFields.users -> false)

  implicit val puzzleBSONHandler = new BSON[Puzzle] {

    import BSONFields._
    import Perf.perfBSONHandler
    import Vote.voteBSONHandler

    def reads(r: BSON.Reader): Puzzle = Puzzle(
      id = r int id,
      gameId = r strO gameId,
      tags = r.get[List[String]](tags),
      history = r str history split ' ' toList,
      fen = r str fen,
      lines = r.get[Lines](lines),
      depth = r int depth,
      color = Color(r bool white),
      date = r date date,
      perf = r.get[Perf](perf),
      vote = r.get[Vote](vote),
      attempts = r int attempts,
      wins = r int wins,
      time = r int time)

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
      perf -> o.perf,
      vote -> o.vote,
      attempts -> o.attempts,
      wins -> o.wins,
      time -> o.time)
  }
}
