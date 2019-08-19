package lila.hub

import com.github.benmanes.caffeine.cache._

import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

final class DuctMap[D <: Duct](
    mkDuct: String => D,
    accessTimeout: FiniteDuration,
    removalListener: Option[D => Unit] = none
) {

  def getOrMake(id: String): D = ducts.get(id)

  def getIfPresent(id: String): Option[D] = Option(ducts getIfPresent id)

  def tell(id: String, msg: Any): Unit = getOrMake(id) ! msg

  def tellAll(msg: Any) = ducts.asMap().asScala.foreach(_._2 ! msg)

  def tellIds(ids: Seq[String], msg: Any): Unit = ids foreach { tell(_, msg) }

  def exists(id: String): Boolean = ducts.getIfPresent(id) != null

  def size: Int = ducts.estimatedSize().toInt

  def kill(id: String): Unit = ducts invalidate id

  private[this] val ducts: LoadingCache[String, D] = {
    val loader = new CacheLoader[String, D] {
      def load(id: String): D = mkDuct(id)
    }
    removalListener match {
      case None => Caffeine.newBuilder()
        .expireAfterAccess(accessTimeout.toMillis, TimeUnit.MILLISECONDS)
        .build[String, D](loader)
      case Some(removal) =>
        Caffeine.newBuilder()
          .expireAfterAccess(accessTimeout.toMillis, TimeUnit.MILLISECONDS)
          .removalListener(new RemovalListener[String, D] {
            def onRemoval(id: String, duct: D, cause: RemovalCause): Unit = removal(duct)
          })
          .build[String, D](loader)
    }
  }
}
