package lila.hub

import java.util.concurrent.ConcurrentHashMap
import java.util.function.{ Function, BiFunction, Consumer }
import ornicar.scalalib.Zero
import scala.concurrent.Promise

final class DuctConcMap[D <: Duct](mkDuct: String => D) extends TellMap {

  def getOrMake(id: String): D = ducts.computeIfAbsent(id, loadFunction)

  def getIfPresent(id: String): Option[D] = Option(ducts get id)

  def tell(id: String, msg: Any): Unit = getOrMake(id) ! msg

  def tellAll(msg: Any) = ducts.forEachValue(16, new Consumer[D] {
    def accept(duct: D) = duct ! msg
  })

  def tellIds(ids: Seq[String], msg: Any): Unit = ids foreach { tell(_, msg) }

  def ask[A](id: String)(makeMsg: Promise[A] => Any): Fu[A] = getOrMake(id).ask(makeMsg)

  def askIfPresent[A](id: String)(makeMsg: Promise[A] => Any): Fu[Option[A]] = getIfPresent(id) ?? {
    _ ask makeMsg map some
  }

  def askIfPresentOrZero[A: Zero](id: String)(makeMsg: Promise[A] => Any): Fu[A] =
    askIfPresent(id)(makeMsg) map (~_)

  def exists(id: String): Boolean = ducts.get(id) != null

  def size: Int = ducts.size()

  def terminate(id: String, lastWill: Duct => Unit): Unit =
    ducts.computeIfPresent(id, new BiFunction[String, D, D] {
      def apply(k: String, duct: D) = {
        lastWill(duct)
        nullD
      }
    })

  def touchOrMake(id: String): Unit = ducts get id

  private[this] val ducts = new ConcurrentHashMap[String, D]

  private val loadFunction = new Function[String, D] {
    def apply(k: String) = mkDuct(k)
  }

  // used to remove entriies
  private var nullD: D = _
}
