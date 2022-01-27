package lila.memo

import org.joda.time.DateTime
import reactivemongo.api.bson._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

/** For slow rate limiters only! */
final class MongoRateLimit[K](
    name: String,
    credits: Int,
    duration: FiniteDuration,
    keyToString: K => String,
    coll: Coll,
    enforce: Boolean,
    log: Boolean
) {
  import RateLimit.Cost

  private def makeClearAt = DateTime.now plusMinutes duration.toMinutes.toInt

  private lazy val logger  = lila.log("ratelimit").branch("mongo").branch(name)
  private lazy val monitor = lila.mon.security.rateLimit(s"mongo.$name")

  private case class Entry(_id: String, v: Int, e: DateTime)
  implicit private val entryBSONHandler = Macros.handler[Entry]

  private def makeDbKey(k: K) = s"ratelimit:$name:${keyToString(k)}"

  def apply[A](k: K, cost: Cost = 1, msg: => String = "")(
      op: => Fu[A]
  )(default: => A)(implicit ec: ExecutionContext): Fu[A] =
    if (cost < 1) op
    else {
      val dbKey = makeDbKey(k)
      coll.one[Entry]($id(dbKey)) flatMap {
        case None =>
          coll.insert.one(Entry(dbKey, cost, makeClearAt)) >> op
        case Some(Entry(_, spent, clearAt)) if spent < credits =>
          coll.update.one($id(dbKey), Entry(dbKey, spent + cost, clearAt), upsert = true) >> op
        case Some(Entry(_, _, clearAt)) if clearAt.isBeforeNow =>
          coll.update.one($id(dbKey), Entry(dbKey, cost, makeClearAt), upsert = true) >> op
        case _ if enforce =>
          if (log) logger.info(s"$credits/$duration $k cost: $cost $msg")
          monitor.increment()
          fuccess(default)
        case _ =>
          op
      }
    }
}

final class MongoRateLimitApi(db: lila.db.Db, config: MemoConfig) {

  private val coll = db(config.cacheColl)

  def apply[K](
      name: String,
      credits: Int,
      duration: FiniteDuration,
      keyToString: K => String = (k: K) => k.toString,
      enforce: Boolean = true,
      log: Boolean = true
  ) = new MongoRateLimit[K](
    credits = credits,
    duration = duration,
    name = name,
    keyToString = keyToString,
    coll = coll,
    enforce = enforce,
    log = log
  )
}
