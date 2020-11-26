package lila.puzzle

import chess.Color
import chess.format.{ FEN, Forsyth, Uci }
import org.joda.time.DateTime
import scala.util.{ Success, Try }

case class Puzzle(
    id: PuzzleId,
    gameId: String,
    history: List[String],
    fen: FEN,
    lines: List[Line],
    depth: Int,
    color: Color,
    date: DateTime,
    perf: PuzzlePerf,
    vote: AggregateVote,
    attempts: Int,
    mate: Boolean
) {

  // ply after "initial move" when we start solving
  def initialPly: Int =
    fen.fullMove ?? { fm =>
      fm * 2 - color.fold(0, 1)
    }

  // (1 - 3)/(1 + 3) = -0.5
  def enabled = vote.ratio > AggregateVote.minRatio || vote.nb < AggregateVote.minVotes

  def withVote(f: AggregateVote => AggregateVote) = copy(vote = f(vote))

  def initialMove: Uci.Move = history.lastOption flatMap Uci.Move.apply err s"Bad initial move $this"

  def fenAfterInitialMove: Option[FEN] =
    for {
      sit1 <- Forsyth << fen
      sit2 <- sit1.move(initialMove).toOption.map(_.situationAfter)
    } yield Forsyth >> sit2
}

object Puzzle {

  case class UserResult(
      puzzleId: PuzzleId,
      userId: lila.user.User.ID,
      result: Result,
      rating: (Int, Int)
  )

  def make(
      gameId: String,
      history: List[String],
      fen: FEN,
      color: Color,
      lines: Lines,
      mate: Boolean
  )(id: PuzzleId) =
    new Puzzle(
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
      mate = mate
    )

  import reactivemongo.api.bson._
  import lila.db.BSON
  import BSON.BSONJodaDateTimeHandler
  implicit private val linesBSONHandler =
    new BSONDocumentReader[Lines] with BSONDocumentWriter[Lines] {
      private def readMove(move: String) =
        chess.Pos.doublePiotrToKey(move take 2) match {
          case Some(m) => s"$m${move drop 2}"
          case _       => sys error s"Invalid piotr move notation: $move"
        }
      def readDocument(doc: BSONDocument): Try[Lines] =
        Try {
          doc.elements.map {
            case BSONElement(move, BSONBoolean(true))  => Win(readMove(move))
            case BSONElement(move, BSONBoolean(false)) => Retry(readMove(move))
            case BSONElement(move, more: BSONDocument) => Node(readMove(move), readDocument(more).get)
            case BSONElement(move, value) =>
              throw new Exception(s"Can't read value of $move: $value")
          } to List
        }
      private def writeMove(move: String) =
        chess.Pos.doubleKeyToPiotr(move take 4) match {
          case Some(m) => s"$m${move drop 4}"
          case _       => sys error s"Invalid move notation: $move"
        }
      def writeTry(lines: Lines): Try[BSONDocument] =
        Success(BSONDocument(lines map {
          case Win(move)         => writeMove(move) -> BSONBoolean(true)
          case Retry(move)       => writeMove(move) -> BSONBoolean(false)
          case Node(move, lines) => writeMove(move) -> writeTry(lines).get
        }))
    }

  object BSONFields {
    val id        = "_id"
    val gameId    = "gameId"
    val history   = "history"
    val fen       = "fen"
    val lines     = "lines"
    val depth     = "depth"
    val white     = "white"
    val date      = "date"
    val perf      = "perf"
    val rating    = s"$perf.gl.r"
    val vote      = "vote"
    val voteNb    = s"$vote.nb"
    val voteRatio = s"$vote.ratio"
    val day       = "day"
    val attempts  = "attempts"
    val mate      = "mate"
  }

  implicit val puzzleBSONHandler = new BSON[Puzzle] {

    import lila.db.dsl.FENHandler
    import BSONFields._
    import PuzzlePerf.puzzlePerfBSONHandler
    import AggregateVote.aggregatevoteBSONHandler

    def reads(r: BSON.Reader): Puzzle =
      Puzzle(
        id = r int id,
        gameId = r str gameId,
        history = r str history split ' ' toList,
        fen = r.get[FEN](fen),
        lines = r.get[Lines](lines),
        depth = r int depth,
        color = Color.fromWhite(r bool white),
        date = r date date,
        perf = r.get[PuzzlePerf](perf),
        vote = r.get[AggregateVote](vote),
        attempts = r int attempts,
        mate = r bool mate
      )

    def writes(w: BSON.Writer, o: Puzzle) =
      BSONDocument(
        id       -> o.id,
        gameId   -> o.gameId,
        history  -> o.history.mkString(" "),
        fen      -> o.fen,
        lines    -> o.lines,
        depth    -> o.depth,
        white    -> o.color.white,
        date     -> o.date,
        perf     -> o.perf,
        vote     -> o.vote,
        attempts -> o.attempts,
        mate     -> o.mate
      )
  }
}
