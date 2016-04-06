package lila.memo

import org.joda.time.DateTime
import reactivemongo.bson._
import reactivemongo.bson.Macros
import scala.concurrent.duration._
import spray.caching.{ LruCache, Cache }

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

final class MongoCache[K, V: MongoCache.Handler] private (
    prefix: String,
    expiresAt: () => DateTime,
    cache: Cache[V],
    coll: Coll,
    f: K => Fu[V],
    keyToString: K => String) {

  def apply(k: K): Fu[V] = cache(k) {
    coll.find(select(k)).uno[Entry] flatMap {
      case None => f(k) flatMap { v =>
        coll.insert(makeEntry(k, v)) recover
          lila.db.recoverDuplicateKey(_ => ()) inject v
      }
      case Some(entry) => fuccess(entry.v)
    }
  }

  def remove(k: K): Funit =
    coll.remove(select(k)).void >>- (cache remove k)

  private case class Entry(_id: String, v: V, e: DateTime)

  private implicit val entryBSONHandler = Macros.handler[Entry]

  private def makeEntry(k: K, v: V) = Entry(makeKey(k), v, expiresAt())

  private def makeKey(k: K) = s"$prefix:${keyToString(k)}"

  private def select(k: K) = BSONDocument("_id" -> makeKey(k))
}

object MongoCache {

  private type Handler[T] = BSONHandler[_ <: BSONValue, T]

  private def expiresAt(ttl: Duration)(): DateTime =
    DateTime.now plusSeconds ttl.toSeconds.toInt

  final class Builder(coll: Coll) {

    def apply[K, V: Handler](
      prefix: String,
      f: K => Fu[V],
      maxCapacity: Int = 512,
      initialCapacity: Int = 64,
      timeToLive: FiniteDuration,
      timeToLiveMongo: Option[FiniteDuration] = None,
      keyToString: K => String): MongoCache[K, V] = new MongoCache[K, V](
      prefix = prefix,
      expiresAt = expiresAt(timeToLiveMongo | timeToLive),
      cache = LruCache(maxCapacity, initialCapacity, timeToLive),
      coll = coll,
      f = f,
      keyToString = keyToString)

    def single[V: Handler](
      prefix: String,
      f: => Fu[V],
      timeToLive: FiniteDuration,
      timeToLiveMongo: Option[FiniteDuration] = None) = new MongoCache[Boolean, V](
      prefix = prefix,
      expiresAt = expiresAt(timeToLiveMongo | timeToLive),
      cache = LruCache(timeToLive = timeToLive),
      coll = coll,
      f = _ => f,
      keyToString = _.toString)
  }

  def apply(coll: Coll) = new Builder(coll)
}
