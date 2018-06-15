package lila.memo

import com.github.blemale.scaffeine.{ Scaffeine, Cache }
import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.bson.Macros
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

final class MongoCache[K, V: MongoCache.Handler] private (
    prefix: String,
    cache: Cache[K, Fu[V]],
    mongoExpiresAt: () => DateTime,
    coll: Coll,
    f: K => Fu[V],
    keyToString: K => String
) {

  def apply(k: K): Fu[V] = cache.get(k, k =>
    coll.find($id(makeKey(k))).uno[Entry] flatMap {
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

  private case class Entry(_id: String, v: V, e: DateTime)

  private implicit val entryBSONHandler = Macros.handler[Entry]

  private def makeKey(k: K) = s"$prefix:${keyToString(k)}"

  private def persist(k: K, v: V): Funit = {
    val mongoKey = makeKey(k)
    coll.update(
      $id(mongoKey),
      Entry(mongoKey, v, mongoExpiresAt()),
      upsert = true
    ).void
  }
}

object MongoCache {

  private type Handler[T] = BSONHandler[_ <: BSONValue, T]

  // expire in mongo 3 seconds before in heap,
  // to make sure the mongo cache is cleared
  // when the heap value expires
  private def mongoExpiresAt(ttl: FiniteDuration): () => DateTime = {
    val seconds = ttl.toSeconds.toInt - 3
    () => DateTime.now plusSeconds seconds
  }

  final class Builder(coll: Coll) {

    def apply[K, V: Handler](
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

    def single[V: Handler](
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
