package lila.hub

import scala.concurrent.duration.FiniteDuration
import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import com.github.benmanes.caffeine.cache.RemovalCause

import actorApi.map._

final class DuctMap[D <: Duct](
    mkDuct: String => D,
    removalListener: (String, D, RemovalCause) => Unit = (id: String, duct: D, cause: RemovalCause) => (),
    accessTimeout: FiniteDuration
) {

  type K = String

  def getOrMake(id: K): D = ducts.get(id, mkDuct)

  def tell(id: K, msg: Any): Unit = getOrMake(id) ! msg

  def tellAll(msg: Any) = ducts.asMap().foreach(_._2 ! msg)

  def tellIds(ids: Seq[K], msg: Any): Unit = ids foreach { tell(_, msg) }

  def ask(id: K, msg: Any): Fu[Any] = getOrMake(id) ? msg

  def exists(id: K): Boolean = ducts.getIfPresent(id).isDefined

  def size: Int = ducts.estimatedSize().toInt

  def `!`: PartialFunction[Any, Unit] = {

    case Get(id) => getOrMake(id)

    case Tell(id, msg) => tell(id, msg)

    // case TellAll(msg) => actors.foreachValue(_ forward msg)

    case TellIds(ids, msg) => tellIds(ids, msg)

    // case Ask(id, msg) => ask(id) pipeTo sender

    // case Exists(id) => sender ! actors.contains(id)
  }

  private[this] val ducts: Cache[K, D] = Scaffeine()
    .expireAfterAccess(accessTimeout)
    .removalListener(removalListener)
    .build[K, D]()
}
