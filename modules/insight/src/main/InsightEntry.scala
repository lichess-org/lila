package lila.insight

import chess.opening.Ecopening
import chess.Color
import org.joda.time.DateTime

import lila.common.{ LilaOpening, LilaOpeningFamily }
import lila.game.{ Game, Pov }
import lila.rating.PerfType
import lila.user.User

case class InsightEntry(
    id: String,  // gameId + w/b
    number: Int, // auto increment over userId
    userId: User.ID,
    color: Color,
    perf: PerfType,
    opening: Option[LilaOpening],
    myCastling: Castling,
    rating: Option[Int],
    opponentRating: Option[Int],
    opponentStrength: Option[RelativeStrength],
    opponentCastling: Castling,
    moves: List[InsightMove],
    queenTrade: QueenTrade,
    result: Result,
    termination: Termination,
    ratingDiff: Int,
    analysed: Boolean,
    provisional: Boolean,
    date: DateTime
) {

  def gameId = id take Game.gameIdSize
}

case object InsightEntry {

  def povToId(pov: Pov) = pov.gameId + pov.color.letter

  object BSONFields {
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
  }
}
