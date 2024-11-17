package lila.recap

import reactivemongo.api.bson.Macros.Annotations.Key
import chess.ByColor
import chess.opening.OpeningFamily
import lila.common.LilaOpeningFamily

case class Recap(
    @Key("_id") id: UserId,
    nbGames: Int,
    openings: ByColor[LilaOpeningFamily],
    results: Recap.Results,
    timePlaying: FiniteDuration,
    createdAt: Instant
)

object Recap:

  case class Results(win: Int, draw: Int, loss: Int)
