package lila.hub

import com.github.benmanes.caffeine.cache._

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

final class DuctMap[D <: Duct](
    mkDuct: String => D,
    accessTimeout: FiniteDuration
) {

  def getOrMake(id: String): D = ducts.get(id)

  def tell(id: String, msg: Any): Unit = getOrMake(id) ! msg

  def tellAll(msg: Any) = ducts.asMap().asScala.foreach(_._2 ! msg)

  def tellIds(ids: Seq[String], msg: Any): Unit = ids foreach { tell(_, msg) }

  def exists(id: String): Boolean = ducts.getIfPresent(id) != null

  def size: Int = ducts.estimatedSize().toInt

  def kill(id: String): Unit = ducts invalidate id

  private[this] val ducts: LoadingCache[String, D] =
    Caffeine.newBuilder()
      .expireAfterAccess(accessTimeout.toMillis, TimeUnit.MILLISECONDS)
      .build[String, D](new CacheLoader[String, D] {
        def load(id: String): D = mkDuct(id)
      })
}
