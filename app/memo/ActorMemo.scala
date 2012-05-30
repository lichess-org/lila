package lila
package memo

import akka.actor._
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import akka.dispatch.{ Future, Await }
import akka.pattern.ask
import akka.util.duration._
import akka.util.Duration
import akka.util.Timeout

import play.api.Play.current
import play.api.libs.concurrent._

import com.google.common.cache.LoadingCache

final class ActorMemo[K, V] private (
    cache: LoadingCache[K, V],
    atMost: Duration,
    keyDef: ActorMemo.KeyDef[K],
    valMan: Manifest[V]) {

  def apply(key: K): V = Await.result(async(key), atMost)

  def async(key: K): Future[V] = actor ? key mapTo manifest[V]

  private implicit val valueManifest = valMan
  private implicit val timeout = Timeout(atMost)

  private val actor = Akka.system.actorOf(Props(new Actor {
    def receive = {
      case keyDef(key) ⇒ sender ! (cache get key)
    }
  }))
}

object ActorMemo {

  def apply[K, V](load: K ⇒ V, ttl: Int, atMost: Duration)(
      implicit keyMan: Manifest[K], valMan: Manifest[V]) = new ActorMemo(
    cache = Builder.cache[K, V](ttl, load),
    atMost = atMost,
    keyDef = new KeyDef[K],
    valMan = valMan)

  final class KeyDef[C](implicit desired: Manifest[C]) {

    def unapply[X](c: X)(implicit m: Manifest[X]): Option[C] = {

      def sameArgs = desired.typeArguments.zip(m.typeArguments).forall {
        case (desired, actual) ⇒ desired >:> actual
      }

      (desired >:> m && sameArgs) option c.asInstanceOf[C]
    }
  }
}
