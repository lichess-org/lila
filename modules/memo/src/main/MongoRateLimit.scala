package lila.memo

import reactivemongo.api.bson.*
import scalalib.actor.AsyncActorSequencers
import scalalib.future.FutureAfter

import lila.db.dsl.{ *, given }

/** For slow rate limiters only! */
final class MongoRateLimit[K](
    name: String,
    credits: Int,
    duration: FiniteDuration,
    keyToString: K => String,
    coll: Coll,
    enforce: Boolean,
    log: Boolean
)(using Executor, FutureAfter):
  import MongoRateLimit.{ *, given }
  import RateLimit.Cost

  private def makeClearAt = nowInstant.plus(duration)

  private val logger = RateLimit.logger.branch("mongo").branch(name)
  private val monitor = lila.mon.security.rateLimit(s"mongo.$name")

  private val sequencer = AsyncActorSequencers[K](
    maxSize = Max(credits),
    expiration = 1.minute,
    timeout = 10.seconds,
    name = s"$name.sequencer",
    lila.log.asyncActorMonitor.highCardinality
  )

  private def makeDbKey(k: K) = s"ratelimit:$name:${keyToString(k)}"

  def getSpent(k: K)(using Executor): Fu[Entry] =
    coll
      .one[Entry]($id(makeDbKey(k)))
      .map:
        case Some(v) => v
        case _ => Entry(k.toString(), 0, makeClearAt)

  def apply[A <: Matchable](k: K, cost: Cost = 1, msg: => String = "")(
      op: => Fu[A]
  )(default: => A)(using Executor): Fu[A] =
    if cost < 1 then op
    else
      sequencer(k):
        val dbKey = makeDbKey(k)
        coll
          .one[Entry]($id(dbKey))
          .flatMap:
            case None =>
              coll.insert.one(Entry(dbKey, cost, makeClearAt)) >> op
            case Some(Entry(_, spent, clearAt)) if spent < credits =>
              coll.update.one($id(dbKey), Entry(dbKey, spent + cost, clearAt), upsert = true) >> op
            case Some(Entry(_, _, clearAt)) if clearAt.isBeforeNow =>
              coll.update.one($id(dbKey), Entry(dbKey, cost, makeClearAt), upsert = true) >> op
            case _ if enforce =>
              if log then logger.info(s"$credits/$duration $k cost: $cost $msg")
              monitor.increment()
              fuccess(default)
            case _ =>
              op

object MongoRateLimit:
  case class Entry(_id: String, v: Int, e: Instant):
    inline def until = e
  private given BSONDocumentHandler[Entry] = Macros.handler[Entry]

final class MongoRateLimitApi(db: lila.db.Db, config: MemoConfig):

  private val coll = db(config.cacheColl)

  def apply[K](
      name: String,
      credits: Int,
      duration: FiniteDuration,
      keyToString: K => String = (k: K) => k.toString,
      enforce: Boolean = true,
      log: Boolean = true
  )(using Executor, FutureAfter) = new MongoRateLimit[K](
    credits = credits,
    duration = duration,
    name = name,
    keyToString = keyToString,
    coll = coll,
    enforce = enforce,
    log = log
  )
