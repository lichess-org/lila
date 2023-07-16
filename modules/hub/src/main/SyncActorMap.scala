package lila.hub

import com.github.blemale.scaffeine.LoadingCache
import com.github.benmanes.caffeine.cache.RemovalCause

final class SyncActorMap[Id, T <: SyncActor](
    mkActor: Id => T,
    accessTimeout: FiniteDuration
):

  def getOrMake(id: Id): T = actors get id

  def touchOrMake(id: Id): Unit = getOrMake(id)

  def getIfPresent(id: Id): Option[T] = actors getIfPresent id

  def tell(id: Id, msg: Matchable): Unit = getOrMake(id) ! msg

  def tellIfPresent(id: Id, msg: => Matchable): Unit = getIfPresent(id).foreach(_ ! msg)

  def ask[A](id: Id)(makeMsg: Promise[A] => Matchable): Fu[A] = getOrMake(id).ask(makeMsg)

  private[this] val actors: LoadingCache[Id, T] =
    lila.common.LilaCache.scaffeine
      .expireAfterAccess(accessTimeout)
      .removalListener((id: Id, actor: T, cause: RemovalCause) => actor.stop())
      .build[Id, T](mkActor)
