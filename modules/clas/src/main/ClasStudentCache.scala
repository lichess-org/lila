package lila.clas

import akka.actor.Scheduler
import akka.stream.Materializer
import akka.stream.scaladsl._
import bloomfilter.mutable.BloomFilter
import play.api.Mode
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
    mat: Materializer,
    mode: Mode
) {

  private val falsePositiveRate = 0.00003
  private var bloomFilter       = BloomFilter[User.ID](10, 0.1) // temporary empty filter

  def isStudent(userId: User.ID) = bloomFilter mightContain userId

  def addStudent(userId: User.ID): Unit = bloomFilter add userId

  private def rebuildBloomFilter(): Unit =
    colls.student.countAll foreach { count =>
      val nextBloom = BloomFilter[User.ID](count + 1, falsePositiveRate)
      colls.student
        .find($doc("archived" $exists false), $doc("userId" -> true).some)
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
        .unit
    }

  scheduler.scheduleWithFixedDelay(23 seconds, 1 hour) { rebuildBloomFilter _ }.unit
}
