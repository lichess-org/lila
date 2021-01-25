package lila.storm

import org.joda.time.DateTime

import lila.user.User
import lila.common.ThreadLocalRandom

case class StormRun(
    _id: String,
    user: User.ID,
    date: DateTime,
    puzzles: Int,
    score: Int,
    moves: Int,
    combo: Int,
    time: Int,
    highest: Int
)

object StormRun {
  import lila.db.dsl._
  import reactivemongo.api.bson._
  implicit val stormGameBSONHandler = Macros.handler[StormRun]

  def randomId = ThreadLocalRandom nextString 8
}
