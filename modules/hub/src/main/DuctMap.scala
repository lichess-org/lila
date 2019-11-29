package lila.hub

import com.github.benmanes.caffeine.cache._
import ornicar.scalalib.Zero

import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Promise

final class DuctMap[+D <: Duct](
    mkDuct: String => D,
    accessTimeout: FiniteDuration
) extends TellMap {

  def getOrMake(id: String): D = ducts get id

  def getIfPresent(id: String): Option[D] = Option(ducts getIfPresent id)

  def tell(id: String, msg: Any): Unit = getOrMake(id) ! msg

  def tellAll(msg: Any) = ducts.asMap().asScala.foreach(_._2 ! msg)

  def tellIds(ids: Seq[String], msg: Any): Unit = ids foreach { tell(_, msg) }

  def ask[A](id: String)(makeMsg: Promise[A] => Any): Fu[A] = getOrMake(id).ask(makeMsg)

  def askIfPresent[A](id: String)(makeMsg: Promise[A] => Any): Fu[Option[A]] = getIfPresent(id) ?? {
    _ ask makeMsg map some
  }

  def askIfPresentOrZero[A: Zero](id: String)(makeMsg: Promise[A] => Any): Fu[A] =
    askIfPresent(id)(makeMsg) map (~_)

  def exists(id: String): Boolean = ducts.getIfPresent(id) != null

  def size: Int = ducts.estimatedSize().toInt

  def kill(id: String): Unit = ducts invalidate id

  def touchOrMake(id: String): Unit = ducts get id

  private[this] val ducts: LoadingCache[String, D] =
    Caffeine.newBuilder()
      .expireAfterAccess(accessTimeout.toMillis, TimeUnit.MILLISECONDS)
      .build[String, D](new CacheLoader[String, D] {
        def load(id: String): D = mkDuct(id)
      })
}
