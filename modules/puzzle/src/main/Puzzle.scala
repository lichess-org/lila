package lidraughts.puzzle

import scala.collection.breakOut
import draughts.Color
import draughts.format.Forsyth.SituationPlus
import draughts.format.{ Forsyth, Uci }
import draughts.variant.{ Variant, Standard }
import org.joda.time.DateTime

case class Puzzle(
    id: PuzzleId,
    variant: Variant,
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
    mate: Boolean
) {

  // ply after "initial move" when we start solving
  def initialPly: Int = {
    fen.split(':').find(_.startsWith("F")) flatMap (m => parseIntOption(m.drop(1))) map { move =>
      move * 2 - color.fold(0, 1)
    }
  } | 0

  // (1 - 3)/(1 + 3) = -0.5
  def enabled = vote.ratio > AggregateVote.minRatio || vote.nb < AggregateVote.minVotes

  def withVote(f: AggregateVote => AggregateVote) = copy(vote = f(vote))

  def initialMove: Uci.Move = history.lastOption flatMap {
    uci =>
      val rawUci = Uci.Move.apply(uci)
      if (rawUci.isEmpty) rawUci
      else
        Forsyth.<<@(variant, fen).fold(rawUci) {
          _.validMoves.get(rawUci.get.orig).fold(rawUci)(
            _.find {
              move => move.dest == rawUci.get.dest
            } match {
              case Some(move) => move.toUci.some
              case _ => rawUci
            }
          )
        }
  } err s"Bad initial move $this"

  def fenAfterInitialMove: Option[String] = {
    for {
      sit1 <- Forsyth.<<@(variant, fen)
      sit2 <- sit1.move(initialMove).toOption.map(_.situationAfter)
    } yield Forsyth >> SituationPlus(sit2, color.fold(2, 1))
  }
}

object Puzzle {

  case class UserResult(
      puzzleId: PuzzleId,
      variant: Variant,
      userId: lidraughts.user.User.ID,
      result: Result,
      rating: (Int, Int)
  )

  def make(
    gameId: String,
    history: List[String],
    fen: String,
    color: Color,
    lines: Lines,
    mate: Boolean
  )(id: PuzzleId, variant: Variant) = new Puzzle(
    id = id,
    variant = variant,
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

  import reactivemongo.bson._
  import lidraughts.db.BSON
  import BSON.BSONJodaDateTimeHandler
  private implicit val linesBSONHandler = new BSONHandler[BSONDocument, Lines] {
    private def readMove(move: String) = draughts.Piotr.doublePiotrToKey(move take 2) match {
      case Some(m) => s"$m${move drop 2}"
      case _ => sys error s"Invalid piotr move notation: $move"
    }
    def read(doc: BSONDocument): Lines = doc.elements.map {
      case BSONElement(move, BSONBoolean(true)) => Win(readMove(move))

      case BSONElement(move, BSONBoolean(false)) => Retry(readMove(move))

      case BSONElement(move, more: BSONDocument) =>
        Node(readMove(move), read(more))

      case BSONElement(move, value) =>
        throw new Exception(s"Can't read value of $move: $value")
    }(breakOut)
    private def writeMove(move: String) = draughts.Piotr.doubleKeyToPiotr(move take 4) match {
      case Some(m) => s"$m${move drop 4}"
      case _ => sys error s"Invalid move notation: $move"
    }
    def write(lines: Lines): BSONDocument = BSONDocument(lines map {
      case Win(move) => writeMove(move) -> BSONBoolean(true)
      case Retry(move) => writeMove(move) -> BSONBoolean(false)
      case Node(move, lines) => writeMove(move) -> write(lines)
    })
  }

  object BSONFields {
    val id = "_id"
    val variant = "variant"
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
    val voteDown = s"$vote.down"
    val voteNb = s"$vote.nb"
    val voteRatio = s"$vote.ratio"
    val day = "day"
    val attempts = "attempts"
    val mate = "mate"
  }

  implicit val puzzleBSONHandler = new BSON[Puzzle] {

    import BSONFields._
    import PuzzlePerf.puzzlePerfBSONHandler
    import AggregateVote.aggregatevoteBSONHandler

    def reads(r: BSON.Reader): Puzzle = Puzzle(
      id = r int id,
      variant = r strO variant flatMap Variant.apply getOrElse Standard,
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
      mate = r bool mate
    )
    def writes(w: BSON.Writer, o: Puzzle) = BSONDocument(
      id -> o.id,
      variant -> o.variant.key,
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
      mate -> o.mate
    )
  }
}
