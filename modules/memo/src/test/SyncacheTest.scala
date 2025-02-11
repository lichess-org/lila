package lila.memo

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.AfterAll

class SyncacheTest extends TestKit(ActorSystem()) with SpecificationLike with AfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  implicit val ec: ExecutionContext = system.dispatcher

  "syncache" should {
    "be thread safe" in {
      var computeCount = 0
      val cache = new Syncache[Int, String](
        name = "test",
        initialCapacity = 64,
        compute = s =>
          Future {
            computeCount += 1
            s"computed $s"
          },
        default = s => s"default $s",
        strategy = Syncache.AlwaysWait(1 second),
        expireAfter = Syncache.ExpireAfterWrite(20 seconds),
        refreshAfter = Syncache.NoRefresh,
      )
      val threads = 20
      val keys    = 50
      val fs = (1 to threads).map { _ =>
        Future {
          (1 to 5) foreach { _ =>
            (1 to keys) foreach { i =>
              cache.sync(i)
            }
          }
        }
      }
      Await.result(Future.sequence(fs), 20.seconds)
      computeCount must beEqualTo(keys)
    }
  }
}
