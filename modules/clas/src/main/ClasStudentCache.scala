package lila.clas

import akka.actor.Scheduler
import akka.stream.Materializer
import akka.stream.scaladsl._
import bloomfilter.mutable.BloomFilter
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

final class ClasStudentCache(colls: ClasColls, cacheApi: CacheApi)(implicit
    ec: ExecutionContext,
    scheduler: Scheduler,
    mat: Materializer
) {

  private val expectedElements  = 200_000
  private val falsePositiveRate = 0.001
  private var bloomFilter       = BloomFilter[User.ID](expectedElements, falsePositiveRate)

  def isStudent(userId: User.ID) = bloomFilter mightContain userId

  private def rebuildBloomFilter(): Unit = {
    val nextBloom = BloomFilter[User.ID](expectedElements, falsePositiveRate)
    colls.student
      .find($empty, $doc("userId" -> true).some)
      .cursor[Bdoc](ReadPreference.secondaryPreferred)
      .documentSource()
      .toMat(Sink.fold[Int, Bdoc](0) { case (total, doc) =>
        doc string "userId" foreach nextBloom.add
        total + 1
      })(Keep.right)
      .run()
      .addEffect { nb =>
        lila.mon.clas.student.bloomFilter.count.update(nb)
        bloomFilter.dispose()
        bloomFilter = nextBloom
      }
      .monSuccess(_.clas.student.bloomFilter.fu)
      .addEffectAnyway {
        scheduler.scheduleOnce(5 minutes) { rebuildBloomFilter() }.unit
      }
      .unit
  }

  scheduler.scheduleOnce(20 seconds) { rebuildBloomFilter() }.unit
}
