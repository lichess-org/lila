package lila.hub

import com.github.benmanes.caffeine.cache._
import java.util.concurrent.TimeUnit
import ornicar.scalalib.Zero
import play.api.Mode
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Promise
import scala.jdk.CollectionConverters._

final class TrouperMap[T <: Trouper](
    mkTrouper: String => T,
    accessTimeout: FiniteDuration
)(implicit mode: Mode) {

  def getOrMake(id: String): T = troupers get id

  def getIfPresent(id: String): Option[T] = Option(troupers getIfPresent id)

  def tell(id: String, msg: Any): Unit = getOrMake(id) ! msg

  def tellIfPresent(id: String, msg: => Any): Unit = getIfPresent(id) foreach (_ ! msg)

  def tellAll(msg: Any) = troupers.asMap.asScala.foreach(_._2 ! msg)

  def tellIds(ids: Seq[String], msg: Any): Unit = ids foreach { tell(_, msg) }

  def ask[A](id: String)(makeMsg: Promise[A] => Any): Fu[A] = getOrMake(id).ask(makeMsg)

  def askIfPresent[A](id: String)(makeMsg: Promise[A] => Any): Fu[Option[A]] =
    getIfPresent(id) ?? {
      _ ask makeMsg dmap some
    }

  def askIfPresentOrZero[A: Zero](id: String)(makeMsg: Promise[A] => Any): Fu[A] =
    askIfPresent(id)(makeMsg) dmap (~_)

  def exists(id: String): Boolean = troupers.getIfPresent(id) != null

  def size: Int = troupers.estimatedSize().toInt

  def kill(id: String): Unit = troupers invalidate id

  def killAll(): Unit = troupers.invalidateAll

  def touch(id: String): Unit = troupers.getIfPresent(id).unit

  def touchOrMake(id: String): Unit = troupers.get(id).unit

  private[this] val troupers: LoadingCache[String, T] =
    lila.common.LilaCache
      .caffeine(mode)
      .recordStats
      .expireAfterAccess(accessTimeout.toMillis, TimeUnit.MILLISECONDS)
      .removalListener(new RemovalListener[String, T] {
        def onRemoval(id: String, trouper: T, cause: RemovalCause): Unit =
          trouper.stop()
      })
      .build[String, T](new CacheLoader[String, T] {
        def load(id: String): T = mkTrouper(id)
      })

  def monitor(name: String) = lila.mon.caffeineStats(troupers, name)

  def keys: Set[String] = troupers.asMap.asScala.keySet.toSet
}
