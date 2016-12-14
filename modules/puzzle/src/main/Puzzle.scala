package lila.puzzle

import chess.Color
import chess.format.{ Uci, Forsyth }
import org.joda.time.DateTime

case class Puzzle(
    id: PuzzleId,
    gameId: String,
    history: List[String],
    fen: String,
    lines: List[Line],
    depth: Int,
    color: Color,
    date: DateTime,
    perf: PuzzlePerf,
    vote: AggregateVote,
    attempts: Int,
    mate: Boolean,
    tags: List[TagVoted]) {

  // ply after "initial move" when we start solving
  def initialPly: Int = {
    fen.split(' ').lastOption flatMap parseIntOption map { move =>
      move * 2 - color.fold(0, 1)
    }
  } | 0

  // (1 - 3)/(1 + 3) = -0.5
  def enabled = vote.ratio > AggregateVote.minRatio || vote.nb < AggregateVote.minVotes

  def withVote(f: AggregateVote => AggregateVote) = copy(vote = f(vote))

  def initialMove: Uci.Move = history.lastOption flatMap Uci.Move.apply err s"Bad initial move $this"

  def fenAfterInitialMove: Option[String] = {
    for {
      sit1 <- Forsyth << fen
      sit2 <- sit1.move(initialMove).toOption.map(_.situationAfter)
    } yield Forsyth >> sit2
  }

  def visibleTags: List[TagVoted] = tags.filter(_.sum > 2)

  def withTagVote(f: List[TagVoted] => List[TagVoted]) = copy(tags = f(tags))

  def trustedTags: List[TagVoted] = tags.filter(_.trusted)
}

object Puzzle {

  def make(
    gameId: String,
    history: List[String],
    fen: String,
    color: Color,
    lines: Lines,
    mate: Boolean)(id: PuzzleId) = new Puzzle(
    id = id,
    gameId = gameId,
    history = history,
    fen = fen,
    lines = lines,
    depth = Line minDepth lines,
    color = color,
    date = DateTime.now,
    perf = PuzzlePerf.default,
    vote = AggregateVote.default,
    attempts = 0,
    mate = mate,
    tags = List())

  import reactivemongo.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  private implicit val lineBSONHandler = new BSONHandler[BSONDocument, Lines] {
    private def readMove(move: String) = chess.Pos.doublePiotrToKey(move take 2) match {
      case Some(m) => s"$m${move drop 2}"
      case _       => sys error s"Invalid piotr move notation: $move"
    }
    def read(doc: BSONDocument): Lines = doc.elements.toList map {
      case BSONElement(move, BSONBoolean(true))  => Win(readMove(move))

      case BSONElement(move, BSONBoolean(false)) => Retry(readMove(move))

      case BSONElement(move, more: BSONDocument) =>
        Node(readMove(move), read(more))

      case BSONElement(move, value) =>
        throw new Exception(s"Can't read value of $move: $value")
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

  private implicit val tagBSONHandler = new BSONHandler[BSONDocument, List[TagVoted]] {
    def read(doc: BSONDocument): List[TagVoted] = ??? /*doc.elements.toList map {
      case BSONDocument(id -> BSONDocument("up" -> up, "down" -> down)) => TagVoted(Tag.byId(id), TagAggregateVote(up, down))
      case _ => throw new Exception(s"malformed BSONDocument")
    }*/
    def write(tags: List[TagVoted]): BSONDocument = BSONDocument(tags map {
      t => BSONDocument(t.tag.id -> BSONDocument("up" -> t.vote.up, "down" -> t.vote.down))
    })
  }

  object BSONFields {
    val id = "_id"
    val gameId = "gameId"
    val history = "history"
    val fen = "fen"
    val lines = "lines"
    val depth = "depth"
    val white = "white"
    val date = "date"
    val perf = "perf"
    val rating = s"$perf.gl.r"
    val vote = "vote"
    val voteNb = s"$vote.nb"
    val voteRatio = s"$vote.ratio"
    val day = "day"
    val attempts = "attempts"
    val mate = "mate"
    val tags = "tags"
  }

  implicit val puzzleBSONHandler = new BSON[Puzzle] {

    import BSONFields._
    import PuzzlePerf.puzzlePerfBSONHandler
    import AggregateVote.aggregatevoteBSONHandler

    def reads(r: BSON.Reader): Puzzle = Puzzle(
      id = r int id,
      gameId = r str gameId,
      history = r str history split ' ' toList,
      fen = r str fen,
      lines = r.get[Lines](lines),
      depth = r int depth,
      color = Color(r bool white),
      date = r date date,
      perf = r.get[PuzzlePerf](perf),
      vote = r.get[AggregateVote](vote),
      attempts = r int attempts,
      mate = r bool mate)

    def writes(w: BSON.Writer, o: Puzzle) = BSONDocument(
      id -> o.id,
      gameId -> o.gameId,
      history -> o.history.mkString(" "),
      fen -> o.fen,
      lines -> o.lines,
      depth -> o.depth,
      white -> o.color.white,
      date -> o.date,
      perf -> o.perf,
      vote -> o.vote,
      attempts -> o.attempts,
      mate -> o.mate)
  }
}
