package lila.memo

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import scala.concurrent.duration._
import scala.concurrent.Future

class MySpec()
    extends TestKit(ActorSystem())
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit def ec = system.dispatcher

  "syncache" must {

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
        expireAfter = Syncache.ExpireAfterWrite(10 seconds)
      )
      val threads = 20
      val keys    = 50
      (1 to threads) foreach { _ =>
        Future {
          (1 to 5) foreach { _ =>
            (1 to keys) foreach { i =>
              cache.sync(i)
            }
          }
        }
      }
      Thread sleep 500
      computeCount should be(keys)
    }

  }
}
