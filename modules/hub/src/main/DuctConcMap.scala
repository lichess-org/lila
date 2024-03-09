package lila.hub

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import ornicar.scalalib.Zero
import scala.concurrent.{ ExecutionContext, Promise }
import scala.jdk.CollectionConverters._

final class DuctConcMap[D <: Duct](
    mkDuct: String => D,
    initialCapacity: Int
) extends TellMap {

  def getOrMake(id: String): D = ducts.computeIfAbsent(id, loadFunction)

  def getIfPresent(id: String): Option[D] = Option(ducts get id)

  def tell(id: String, msg: Any): Unit = getOrMake(id) ! msg

  def tellIfPresent(id: String, msg: => Any): Unit = getIfPresent(id) foreach (_ ! msg)

  def tellAll(msg: Any) =
    ducts.forEachValue(16, _ ! msg)

  def tellIds(ids: Seq[String], msg: Any): Unit = ids foreach { tell(_, msg) }

  def ask[A](id: String)(makeMsg: Promise[A] => Any): Fu[A] = getOrMake(id).ask(makeMsg)

  def askIfPresent[A](id: String)(makeMsg: Promise[A] => Any): Fu[Option[A]] =
    getIfPresent(id) ?? {
      _ ask makeMsg dmap some
    }

  def askIfPresentOrZero[A: Zero](id: String)(makeMsg: Promise[A] => Any): Fu[A] =
    askIfPresent(id)(makeMsg) dmap (~_)

  def exists(id: String): Boolean = ducts.get(id) != null

  def foreachKey(f: String => Unit): Unit =
    ducts.forEachKey(16, k => f(k))

  def tellAllWithAck(makeMsg: Promise[Unit] => Any)(implicit ec: ExecutionContext): Fu[Int] =
    ducts.values.asScala
      .map(_ ask makeMsg)
      .sequenceFu
      .map(_.size)

  def size: Int = ducts.size()

  def terminate(id: String, lastWill: Duct => Unit): Unit =
    ducts
      .computeIfPresent(
        id,
        (_, d) => {
          lastWill(d)
          nullD
        }
      )
      .unit

  private[this] val ducts = new ConcurrentHashMap[String, D](initialCapacity)

  private val loadFunction = new Function[String, D] {
    def apply(k: String) = mkDuct(k)
  }

  // used to remove entries
  @scala.annotation.nowarn
  private[this] var nullD: D = _
}
