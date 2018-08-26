package lila.hub

import akka.actor._
import akka.pattern._
import com.github.benmanes.caffeine.cache._
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

final class ActorMapNew(
    mkActor: String => Actor,
    accessTimeout: FiniteDuration,
    name: String,
    system: ActorSystem
) {
  import makeTimeout.large

  def getOrMake(id: String): ActorRef = actors.get(id)

  def tell(id: String, msg: Any): Unit = actors.get(id) ! msg

  def tellAll(msg: Any) = actors.asMap().asScala.foreach(_._2 ! msg)

  def tellIds(ids: Seq[String], msg: Any): Unit = ids foreach { tell(_, msg) }

  def ask[A: Manifest](id: String, msg: Any): Fu[A] =
    new AskableActorRef(getOrMake(id)) ? msg mapTo manifest[A]

  def exists(id: String): Boolean = actors.getIfPresent(id) != null

  def size: Int = actors.estimatedSize().toInt

  def kill(id: String): Unit = actors invalidate id

  private[this] val actors: LoadingCache[String, ActorRef] =
    Caffeine.newBuilder()
      .expireAfterAccess(accessTimeout.toMillis, TimeUnit.MILLISECONDS)
      .removalListener(new RemovalListener[String, ActorRef] {
        def onRemoval(id: String, ref: ActorRef, cause: RemovalCause): Unit = system stop ref
      })
      .build[String, ActorRef](new CacheLoader[String, ActorRef] {
        def load(id: String): ActorRef = try {
          spawn(id, id)
        } catch {
          case e: akka.actor.InvalidActorNameException =>
            lila.log("hub").warn(s"ActorMap $name mkActor", e)
            import ornicar.scalalib.Random.nextString
            spawn(id, s"$id.${nextString(4)}")
        }
      })

  private[this] def spawn(id: String, actorName: String) =
    system.actorOf(Props(mkActor(id)), name = s"$name.$actorName")
}
