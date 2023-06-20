package lila.insight

import chess.Color

import lila.common.SimpleOpening
import lila.game.Pov
import lila.rating.PerfType

case class InsightEntry(
    id: String, // gameId + w/b
    userId: UserId,
    color: Color,
    perf: PerfType,
    opening: Option[SimpleOpening],
    myCastling: Castling,
    rating: Option[IntRating],         // stable rating only
    opponentRating: Option[IntRating], // stable rating only
    opponentStrength: Option[RelativeStrength],
    opponentCastling: Castling,
    moves: List[InsightMove],
    queenTrade: QueenTrade,
    result: Result,
    termination: Termination,
    ratingDiff: IntRatingDiff,
    analysed: Boolean,
    provisional: Boolean,
    date: Instant
)

case object InsightEntry:

  def povToId(pov: Pov) = s"${pov.gameId}${pov.color.letter}"

  object BSONFields:
    val id                       = "_id"
    val number                   = "n"
    val userId                   = "u"
    val color                    = "c"
    val perf                     = "p"
    val opening                  = "op"
    val openingFamily            = "of"
    val myCastling               = "mc"
    val rating                   = "mr"
    val opponentRating           = "or"
    val opponentStrength         = "os"
    val opponentCastling         = "oc"
    val moves: String            = "m"
    def moves(f: String): String = s"$moves.$f"
    val queenTrade               = "q"
    val result                   = "r"
    val termination              = "t"
    val ratingDiff               = "rd"
    val analysed                 = "a"
    val provisional              = "pr"
    val date                     = "d"
