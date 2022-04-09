package lila.hub

import com.github.blemale.scaffeine.LoadingCache
import com.github.benmanes.caffeine.cache.RemovalCause
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Promise

final class SyncActorMap[T <: SyncActor](
    mkActor: String => T,
    accessTimeout: FiniteDuration
) {

  def getOrMake(id: String): T = actors get id

  def touchOrMake(id: String): Unit = getOrMake(id).unit

  def getIfPresent(id: String): Option[T] = actors getIfPresent id

  def tell(id: String, msg: Any): Unit = getOrMake(id) ! msg

  def tellIfPresent(id: String, msg: => Any): Unit = getIfPresent(id) foreach (_ ! msg)

  def ask[A](id: String)(makeMsg: Promise[A] => Any): Fu[A] = getOrMake(id).ask(makeMsg)

  private[this] val actors: LoadingCache[String, T] =
    lila.common.LilaCache.scaffeine
      .expireAfterAccess(accessTimeout)
      .removalListener((id: String, actor: T, cause: RemovalCause) => actor.stop())
      .build[String, T](mkActor)
}
