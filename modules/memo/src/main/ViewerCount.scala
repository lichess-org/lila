package lila.memo

import bloomfilter.mutable.BloomFilter
import lila.core.net.UserAgent
import lila.core.userId.UserId
import lila.core.config.CollName
import lila.db.dsl.*
import lila.common.HTTPRequest

/* Counts approximate unique viewers of a given resource.
 * Uses a bloom filter on (IP, UserAgent, UserId) to prevent duplicate counts.
 * The count is significantly underestimated, due to anonymous users,
 * and to bloom filter false positives.
 */

private final class ViewerCount(initialCount: Int, maxCount: Int):

  import ViewerCount.*

  private val bloom = BloomFilter[String](maxCount, 0.001)

  private var count: Int = initialCount

  def hit(a: Viewer): Unit =
    val s = encode(a)
    if !bloom.mightContain(s) then
      bloom.add(s)
      count += 1

  def get: Int = count

private object ViewerCount:

  type CountKey = String
  type IP = String
  type Viewer = (IP, UserAgent, Option[UserId])

  val encode: Viewer => String =
    case (ip, ua, id) =>
      s"${ip}${ua.value}${id.map(_.value).getOrElse("")}"

final class ViewerCountApi(db: lila.db.Db, cacheApi: CacheApi)(using scheduler: Scheduler)(using Executor):

  import ViewerCount.*

  private val coll = db(CollName("viewer_count"))

  private val cache = cacheApi.notLoading[CountKey, ViewerCount](64, "viewerCount"):
    _.expireAfterAccess(20.minutes).buildAsync()

  private def fetch(key: CountKey): Fu[Int] =
    coll.primitiveOne[Int]($id(key), "v").dmap(_.orZero)

  private def build(key: CountKey, maxCount: Int) =
    fetch(key).map(ViewerCount(_, maxCount))

  def get(key: CountKey): Fu[Int] =
    cache.getIfPresent(key).fold(fetch(key))(_.dmap(_.get))

  def hit(key: CountKey, maxCount: Int)(viewer: Viewer): Unit =
    cache.getFuture(key, build(_, maxCount)).foreach(_.hit(viewer))

  import play.api.mvc.RequestHeader
  def hit(key: CountKey, maxCount: Int)(req: RequestHeader, user: Option[UserId]): Unit =
    val viewer = (req.remoteAddress, HTTPRequest.userAgent(req), user)
    hit(key, maxCount)(viewer)

  scheduler.scheduleWithFixedDelay(2.minutes, 2.minutes): () =>
    cache.underlying.synchronous
      .asMap()
      .forEach: (key, vc) =>
        println(s"ViewerCountApi: persist $key -> ${vc.get}")
        coll.update.one($id(key), $set("v" -> vc.get), upsert = true)
