package lila.app
package memo

import akka.actor._
import scala.concurrent.{ Future, Await }
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent._

import com.google.common.cache.LoadingCache

final class ActorMemo[K, V] private (
    cache: LoadingCache[K, V],
    atMost: FiniteDuration,
    valMan: Manifest[V]) {

  def apply(key: K): V = Await.result(async(key), atMost)

  def async(key: K): Future[V] = actor ? key mapTo manifest[V]

  private implicit val valueManifest = valMan
  private implicit val timeout = Timeout(atMost)

  private val actor = Akka.system.actorOf(Props(new Actor {
    def receive = {
      case key ⇒ sender ! (cache get key.asInstanceOf[K])
    }
  }))
}

object ActorMemo {

  def apply[K, V](load: K ⇒ V, ttl: Int, atMost: FiniteDuration)(
      implicit valMan: Manifest[V]) = new ActorMemo(
    cache = Builder.cache[K, V](ttl, load),
    atMost = atMost,
    valMan = valMan)
}
