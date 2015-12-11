package lila.tournament

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._
import scala.concurrent.duration._

import chess.variant.Variant
import lila.db.BSON._
import lila.db.Types.Coll

final class LeaderboardApi(coll: Coll) {

  import LeaderboardApi._
  import BSONHandlers._
}

object LeaderboardApi {

  case class Entry(
    id: String, // same as tournament player id
    userId: String,
    tourId: String,
    score: Int,
    rank: Int,
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    variant: Variant,
    date: DateTime)
}
