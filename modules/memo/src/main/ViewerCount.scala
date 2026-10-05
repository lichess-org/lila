package lila.memo

import play.api.mvc.RequestHeader
import se.thanh.pds.bloomfilter.BloomFilter
import scalalib.net.UserAgent

import lila.core.userId.UserId
import lila.core.config.CollName
import lila.db.dsl.*
import lila.common.HTTPRequest

/* Counts approximate unique viewers of a given resource.
 * Uses a bloom filter on (IP, UserAgent, UserId) to prevent duplicate counts.
 * The count is significantly underestimated, due to anonymous users,
 * and to bloom filter false positives (1%).
 */

final class ViewerCount(initialCount: Int, maxCount: Int, fpRate: Double = 0.01):

  import ViewerCount.*

  private var bloom = BloomFilter[String](maxCount, fpRate)
  private var count: Int = initialCount

  def hit(a: Viewer): Unit =
    val s = encode(a)
    if !bloom.contains(s) then
      bloom.add(s)
      count += 1

  def get: Int = (count * (1 + fpRate)).toInt

  def reset(): Unit =
    count = 0
    bloom = BloomFilter[String](maxCount, fpRate)

object ViewerCount:

  type CountKey = String
  type IP = String
  type Viewer = (IP, UserAgent, Option[UserId])

  val encode: Viewer => String =
    case (ip, ua, id) =>
      s"${ip}${id.so(_.value)}${ua.value}"

  def makeViewer(req: RequestHeader, user: Option[UserId]): Viewer =
    (req.remoteAddress, HTTPRequest.userAgent(req), user)

final class ViewerCountApi(db: lila.db.Db, cacheApi: CacheApi)(using scheduler: Scheduler)(using Executor):

  import ViewerCount.*

  private val ttl = 20.minutes

  private val coll = db(CollName("viewer_count"))

  private val cache = cacheApi.notLoading[CountKey, ViewerCount](512, "viewerCount"):
    _.expireAfterAccess(ttl).buildAsync()

  private def fetch(key: CountKey): Fu[Int] =
    coll.primitiveOne[Int](bid(key), "v").dmap(_.orZero)

  private def build(key: CountKey, maxCount: Int) =
    fetch(key).map(ViewerCount(_, maxCount))

  def get(key: CountKey): Fu[Int] =
    cache.getIfPresent(key).fold(fetch(key))(_.dmap(_.get))

  def hit(key: CountKey, maxCount: Int)(viewer: Viewer): Unit =
    cache.getFuture(key, build(_, maxCount)).foreach(_.hit(viewer))

  def hit(key: CountKey, maxCount: Int)(req: RequestHeader, user: Option[UserId]): Unit =
    hit(key, maxCount)(makeViewer(req, user))

  scheduler.scheduleWithFixedDelay(ttl / 2, ttl / 2): () =>
    cache.underlying.synchronous
      .asMap()
      .forEach: (key, vc) =>
        coll.update.one(bid(key), set("v" -> vc.get), upsert = true)
