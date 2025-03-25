package lila.memo

import com.github.blemale.scaffeine.AsyncLoadingCache
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

import CacheApi.*

/** To avoid recomputing very expensive values after deploy
  */
final class MongoCache[K, V: BSONHandler] private (
    name: String,
    dbTtl: FiniteDuration,
    keyToString: K => String,
    build: MongoCache.LoaderWrapper[K, V] => AsyncLoadingCache[K, V],
    val coll: Coll
)(using Executor):

  private case class Entry(_id: String, v: V, e: Instant)

  private given BSONDocumentHandler[Entry] = Macros.handler[Entry]

  private val cache = build { loader => k =>
    val dbKey = makeDbKey(k)
    coll
      .one[Entry]($id(dbKey))
      .flatMap:
        case None =>
          lila.mon.mongoCache.request(name, hit = false).increment()
          loader(k)
            .flatMap { v =>
              coll.update
                .one(
                  $id(dbKey),
                  Entry(dbKey, v, nowInstant.plus(dbTtl)),
                  upsert = true
                )
                .inject(v)
            }
            .mon(_.mongoCache.compute(name))
        case Some(entry) =>
          lila.mon.mongoCache.request(name, hit = true).increment()
          fuccess(entry.v)
  }

  def get = cache.get

  def invalidate(key: K): Funit =
    for _ <- coll.delete.one($id(makeDbKey(key)))
    yield cache.invalidate(key)

  private def makeDbKey(key: K) = s"$name:${keyToString(key)}"

object MongoCache:

  type Loader[K, V]        = K => Fu[V]
  type LoaderWrapper[K, V] = Loader[K, V] => Loader[K, V]

  final class Api(
      db: lila.db.Db,
      config: MemoConfig,
      cacheApi: CacheApi
  )(using Executor):

    private val coll = db(config.cacheColl)

    // AsyncLoadingCache with monitoring and DB persistence
    def apply[K, V: BSONHandler](
        initialCapacity: Int,
        name: String,
        dbTtl: FiniteDuration,
        keyToString: K => String
    )(build: LoaderWrapper[K, V] => Builder => AsyncLoadingCache[K, V]): MongoCache[K, V] =
      val cache = MongoCache(
        name,
        dbTtl,
        keyToString,
        (wrapper: LoaderWrapper[K, V]) =>
          build(wrapper)(
            scaffeine.recordStats().initialCapacity(cacheApi.actualCapacity(initialCapacity))
          ),
        coll
      )
      cacheApi.monitor(name, cache.cache)
      cache

    // no in-heap cache
    def noHeap[K, V: BSONHandler](
        name: String,
        dbTtl: FiniteDuration,
        keyToString: K => String
    )(f: K => Fu[V]): MongoCache[K, V] =
      apply[K, V](8, name, dbTtl, keyToString): loader =>
        _.expireAfterWrite(1.second).buildAsyncFuture(loader(f))

    // AsyncLoadingCache for single entry with DB persistence
    def unit[V: BSONHandler](
        name: String,
        dbTtl: FiniteDuration
    )(build: LoaderWrapper[Unit, V] => Builder => AsyncLoadingCache[Unit, V]): MongoCache[Unit, V] =
      MongoCache(
        name,
        dbTtl,
        _ => "",
        wrapper => build(wrapper)(scaffeine.initialCapacity(1)),
        coll
      )

    // no in-heap cache
    def unitNoHeap[V: BSONHandler](
        name: String,
        dbTtl: FiniteDuration
    )(f: Unit => Fu[V]): MongoCache[Unit, V] =
      unit[V](name, dbTtl): loader =>
        _.expireAfterWrite(1.second).buildAsyncFuture(loader(f))
