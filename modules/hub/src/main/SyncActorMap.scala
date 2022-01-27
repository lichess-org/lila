package lila.hub

import com.github.benmanes.caffeine.cache._
import java.util.concurrent.TimeUnit
import ornicar.scalalib.Zero
import play.api.Mode
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Promise
import scala.jdk.CollectionConverters._

final class SyncActorMap[T <: SyncActor](
    mkActor: String => T,
    accessTimeout: FiniteDuration
)(implicit mode: Mode) {

  def getOrMake(id: String): T = actors get id

  def getIfPresent(id: String): Option[T] = Option(actors getIfPresent id)

  def tell(id: String, msg: Any): Unit = getOrMake(id) ! msg

  def tellIfPresent(id: String, msg: => Any): Unit = getIfPresent(id) foreach (_ ! msg)

  def tellAll(msg: Any) = actors.asMap.asScala.foreach(_._2 ! msg)

  def tellIds(ids: Seq[String], msg: Any): Unit = ids foreach { tell(_, msg) }

  def ask[A](id: String)(makeMsg: Promise[A] => Any): Fu[A] = getOrMake(id).ask(makeMsg)

  def askIfPresent[A](id: String)(makeMsg: Promise[A] => Any): Fu[Option[A]] =
    getIfPresent(id) ?? {
      _ ask makeMsg dmap some
    }

  def askIfPresentOrZero[A: Zero](id: String)(makeMsg: Promise[A] => Any): Fu[A] =
    askIfPresent(id)(makeMsg) dmap (~_)

  def exists(id: String): Boolean = actors.getIfPresent(id) != null

  def size: Int = actors.estimatedSize().toInt

  def kill(id: String): Unit = actors invalidate id

  def killAll(): Unit = actors.invalidateAll()

  def touch(id: String): Unit = actors.getIfPresent(id).unit

  def touchOrMake(id: String): Unit = actors.get(id).unit

  private[this] val actors: LoadingCache[String, T] =
    lila.common.LilaCache
      .caffeine(mode)
      .recordStats
      .expireAfterAccess(accessTimeout.toMillis, TimeUnit.MILLISECONDS)
      .removalListener(new RemovalListener[String, T] {
        def onRemoval(id: String, actor: T, cause: RemovalCause): Unit =
          actor.stop()
      })
      .build[String, T](new CacheLoader[String, T] {
        def load(id: String): T = mkActor(id)
      })

  def monitor(name: String) = lila.mon.caffeineStats(actors, name)

  def keys: Set[String] = actors.asMap.asScala.keySet.toSet
}
