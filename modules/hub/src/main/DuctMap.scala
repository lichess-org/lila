package lila.hub

import scala.concurrent.duration.FiniteDuration
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import com.github.benmanes.caffeine.cache.RemovalCause

import actorApi.map._

final class DuctMap[D <: Duct](
    mkDuct: String => D,
    accessTimeout: FiniteDuration
) {

  def getOrMake(id: String): D = ducts.get(id, mkDuct)

  def tell(id: String, msg: Any): Unit = getOrMake(id) ! msg

  def tellAll(msg: Any) = ducts.asMap().foreach(_._2 ! msg)

  def tellIds(ids: Seq[String], msg: Any): Unit = ids foreach { tell(_, msg) }

  def exists(id: String): Boolean = ducts.getIfPresent(id).isDefined

  def size: Int = ducts.estimatedSize().toInt

  def kill(id: String): Unit = ducts invalidate id

  private[this] val ducts: Cache[String, D] = Scaffeine()
    .expireAfterAccess(accessTimeout)
    .build[String, D]()
}
