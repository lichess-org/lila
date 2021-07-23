package lila.hub

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import ornicar.scalalib.Zero
import scala.concurrent.{ ExecutionContext, Promise }
import scala.jdk.CollectionConverters._

final class AsyncActorConcMap[D <: AsyncActor](
    mkAsyncActor: String => D,
    initialCapacity: Int
) extends TellMap {

  def getOrMake(id: String): D = asyncActors.computeIfAbsent(id, loadFunction)

  def getIfPresent(id: String): Option[D] = Option(asyncActors get id)

  def tell(id: String, msg: Any): Unit = getOrMake(id) ! msg

  def tellIfPresent(id: String, msg: => Any): Unit = getIfPresent(id) foreach (_ ! msg)

  def tellAll(msg: Any) =
    asyncActors.forEachValue(16, _ ! msg)

  def tellIds(ids: Seq[String], msg: Any): Unit = ids foreach { tell(_, msg) }

  def ask[A](id: String)(makeMsg: Promise[A] => Any): Fu[A] = getOrMake(id).ask(makeMsg)

  def askIfPresent[A](id: String)(makeMsg: Promise[A] => Any): Fu[Option[A]] =
    getIfPresent(id) ?? {
      _ ask makeMsg dmap some
    }

  def askIfPresentOrZero[A: Zero](id: String)(makeMsg: Promise[A] => Any): Fu[A] =
    askIfPresent(id)(makeMsg) dmap (~_)

  def exists(id: String): Boolean = asyncActors.get(id) != null

  def foreachKey(f: String => Unit): Unit =
    asyncActors.forEachKey(16, k => f(k))

  def tellAllWithAck(makeMsg: Promise[Unit] => Any)(implicit ec: ExecutionContext): Fu[Int] =
    asyncActors.values.asScala
      .map(_ ask makeMsg)
      .sequenceFu
      .map(_.size)

  def size: Int = asyncActors.size()

  def terminate(id: String, lastWill: AsyncActor => Unit): Unit =
    asyncActors
      .computeIfPresent(
        id,
        (_, d) => {
          lastWill(d)
          nullD
        }
      )
      .unit

  private[this] val asyncActors = new ConcurrentHashMap[String, D](initialCapacity)

  private val loadFunction = new Function[String, D] {
    def apply(k: String) = mkAsyncActor(k)
  }

  // used to remove entries
  private[this] var nullD: D = _
}
