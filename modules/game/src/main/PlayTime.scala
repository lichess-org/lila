package lila.game

import lila.db.ByteArray
import lila.db.dsl._
import lila.user.{ User, UserRepo }

import reactivemongo.bson._
import reactivemongo.api.ReadPreference

final class PlayTime(gameColl: Coll) {

  import Game.{ BSONFields => F }

  def apply(user: User): Fu[Option[User.PlayTime]] = user.playTime match {
    case None => compute(user) /* addEffect { _ foreach UserRepo.setPlayTime(user, _) } */
    case pt => fuccess(pt)
  }

  private def compute(user: User): Fu[Option[User.PlayTime]] =
    computeNow(user) map some

  private def extractSeconds(docs: Iterable[Bdoc], onTv: Boolean): Int = ~docs.collectFirst {
    case doc if doc.getAs[Boolean]("_id").has(onTv) =>
      doc.getAs[Long]("ms") map { micros => (micros / 1000000).toInt }
  }.flatten

  private def computeNow(user: User): Fu[User.PlayTime] = {
    import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
    gameColl.aggregateWithReadPreference(Match($doc(
      F.playerUids -> user.id,
      F.clock $exists true
    )), List(
      Project($doc(
        F.id -> false,
        "tv" -> $doc("$gt" -> $arr("$tv", BSONNull)),
        "ms" -> $doc("$subtract" -> $arr("$ua", "$ca"))
      )),
      GroupField("tv")("ms" -> SumField("ms"))
    ), ReadPreference.secondaryPreferred).map { res =>
      val docs = res.firstBatch
      val onTvSeconds = extractSeconds(docs, true)
      val offTvSeconds = extractSeconds(docs, false)
      User.PlayTime(total = onTvSeconds + offTvSeconds, tv = onTvSeconds)
    }
  }
}
