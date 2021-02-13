package lila.memo

import com.github.blemale.scaffeine.AsyncLoadingCache
import org.joda.time.DateTime
import reactivemongo.api.bson._
import scala.concurrent.duration._

import CacheApi._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._

/**
  * To avoid recomputing very expensive values after deploy
  */
final class MongoCache[K, V: BSONHandler] private (
    name: String,
    dbTtl: FiniteDuration,
    keyToString: K => String,
    build: MongoCache.LoaderWrapper[K, V] => AsyncLoadingCache[K, V],
    val coll: Coll
)(implicit ec: scala.concurrent.ExecutionContext) {

  private case class Entry(_id: String, v: V, e: DateTime)

  implicit private val entryBSONHandler = Macros.handler[Entry]

  private val cache = build { loader => k =>
    val dbKey = makeDbKey(k)
    coll.one[Entry]($id(dbKey)) flatMap {
      case None =>
        lila.mon.mongoCache.request(name, hit = false).increment()
        loader(k)
          .flatMap { v =>
            coll.update.one(
              $id(dbKey),
              Entry(dbKey, v, DateTime.now.plusSeconds(dbTtl.toSeconds.toInt)),
              upsert = true
            ) inject v
          }
          .mon(_.mongoCache.compute(name))
      case Some(entry) =>
        lila.mon.mongoCache.request(name, hit = true).increment()
        fuccess(entry.v)
    }
  }

  def get = cache.get _

  def invalidate(key: K): Funit =
    coll.delete.one($id(makeDbKey(key))).void >>-
      cache.invalidate(key)

  private def makeDbKey(key: K) = s"$name:${keyToString(key)}"
}

object MongoCache {

  type Loader[K, V]        = K => Fu[V]
  type LoaderWrapper[K, V] = Loader[K, V] => Loader[K, V]

  final class Api(
      db: lila.db.Db,
      config: MemoConfig,
      cacheApi: CacheApi,
      mode: play.api.Mode
  )(implicit ec: scala.concurrent.ExecutionContext) {

    private val coll = db(config.cacheColl)

    // AsyncLoadingCache with monitoring and DB persistence
    def apply[K, V: BSONHandler](
        initialCapacity: Int,
        name: String,
        dbTtl: FiniteDuration,
        keyToString: K => String
    )(
        build: LoaderWrapper[K, V] => Builder => AsyncLoadingCache[K, V]
    ): MongoCache[K, V] = {
      val cache = new MongoCache(
        name,
        dbTtl,
        keyToString,
        (wrapper: LoaderWrapper[K, V]) =>
          build(wrapper)(
            scaffeine(mode).recordStats().initialCapacity(cacheApi.actualCapacity(initialCapacity))
          ),
        coll
      )
      cacheApi.monitor(name, cache.cache)
      cache
    }

    // AsyncLoadingCache for single entry with DB persistence
    def unit[V: BSONHandler](
        name: String,
        dbTtl: FiniteDuration
    )(
        build: LoaderWrapper[Unit, V] => Builder => AsyncLoadingCache[Unit, V]
    ): MongoCache[Unit, V] =
      new MongoCache(
        name,
        dbTtl,
        _ => "",
        wrapper => build(wrapper)(scaffeine(mode).initialCapacity(1)),
        coll
      )

    // no in-heap cache
    def only[K, V: BSONHandler](
        name: String,
        dbTtl: FiniteDuration,
        keyToString: K => String
    )(f: K => Fu[V]): MongoCache[K, V] =
      apply[K, V](8, name, dbTtl, keyToString) { loader =>
        _.expireAfterWrite(1 second)
          .buildAsyncFuture(loader(f))
      }
  }
}
