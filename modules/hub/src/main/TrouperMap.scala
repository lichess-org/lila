package lila.hub

import com.github.benmanes.caffeine.cache._

import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration

final class TrouperMap[T <: Trouper](
    mkTrouper: String => T,
    accessTimeout: FiniteDuration
) {

  def getOrMake(id: String): T = troupers.get(id)

  def tell(id: String, msg: Any): Unit = getOrMake(id) ! msg

  def tellAll(msg: Any) = troupers.asMap().asScala.foreach(_._2 ! msg)

  def tellIds(ids: Seq[String], msg: Any): Unit = ids foreach { tell(_, msg) }

  def ask[T](id: String)(makeMsg: Promise[T] => Any): Fu[T] = {
    val promise = Promise[T]
    tell(id, makeMsg(promise))
    promise.future
  }

  def exists(id: String): Boolean = troupers.getIfPresent(id) != null

  def size: Int = troupers.estimatedSize().toInt

  def kill(id: String): Unit = troupers invalidate id

  private[this] val troupers: LoadingCache[String, T] =
    Caffeine.newBuilder()
      .expireAfterAccess(accessTimeout.toMillis, TimeUnit.MILLISECONDS)
      .removalListener(new RemovalListener[String, T] {
        def onRemoval(id: String, trouper: T, cause: RemovalCause): Unit =
          trouper.stop()
      })
      .build[String, T](new CacheLoader[String, T] {
        def load(id: String): T = {
          val t = mkTrouper(id)
          t.start()
          t
        }
      })
}
