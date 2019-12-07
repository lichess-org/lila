package lila.memo

import com.github.blemale.scaffeine.{ Scaffeine, Cache }
import org.joda.time.DateTime
import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

final class MongoCache[K, V: BSONHandler] private (
    prefix: String,
    cache: Cache[K, Fu[V]],
    mongoExpiresAt: () => DateTime,
    val coll: Coll,
    f: K => Fu[V],
    keyToString: K => String
) {

  private case class Entry(_id: String, v: V, e: DateTime)

  private implicit val entryBSONHandler = Macros.handler[Entry]

  def apply(k: K): Fu[V] = cache.get(k, k =>
    coll.find($id(makeKey(k)), none[Bdoc]).one[Entry] flatMap {
      case None => f(k) flatMap { v =>
        persist(k, v) inject v
      }
      case Some(entry) => fuccess(entry.v)
    })

  def remove(k: K): Funit = {
    val fut = f(k)
    cache.put(k, fut)
    fut flatMap { v =>
      persist(k, v).void
    }
  }

  private def makeKey(k: K) = s"$prefix:${keyToString(k)}"

  private def persist(k: K, v: V): Funit = {
    val mongoKey = makeKey(k)
    coll.update.one(
      $id(mongoKey),
      Entry(mongoKey, v, mongoExpiresAt()),
      upsert = true
    ).void
  }
}

object MongoCache {

  // expire in mongo 3 seconds before in heap,
  // to make sure the mongo cache is cleared
  // when the heap value expires
  private def mongoExpiresAt(ttl: FiniteDuration): () => DateTime = {
    val seconds = ttl.toSeconds.toInt - 3
    () => DateTime.now plusSeconds seconds
  }

  final class Builder(db: lila.db.Db, config: MemoConfig) {

    val coll = db(config.cacheColl)

    def apply[K, V: BSONHandler](
      prefix: String,
      f: K => Fu[V],
      maxCapacity: Int = 1024,
      timeToLive: FiniteDuration,
      timeToLiveMongo: Option[FiniteDuration] = None,
      keyToString: K => String
    ): MongoCache[K, V] = new MongoCache[K, V](
      prefix = prefix,
      cache = Scaffeine()
        .expireAfterWrite(timeToLive)
        .maximumSize(maxCapacity)
        .build[K, Fu[V]],
      mongoExpiresAt = mongoExpiresAt(timeToLiveMongo | timeToLive),
      coll = coll,
      f = f,
      keyToString = keyToString
    )

    def single[V: BSONHandler](
      prefix: String,
      f: => Fu[V],
      timeToLive: FiniteDuration
    ) = new MongoCache[Unit, V](
      prefix = prefix,
      cache = Scaffeine()
        .expireAfterWrite(timeToLive)
        .maximumSize(1)
        .build[Unit, Fu[V]],
      mongoExpiresAt = mongoExpiresAt(timeToLive),
      coll = coll,
      f = _ => f,
      keyToString = _.toString
    )
  }
}
