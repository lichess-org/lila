package lila.clas

import akka.actor.Scheduler
import akka.stream.Materializer
import akka.stream.scaladsl.*
import bloomfilter.mutable.BloomFilter
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.user.User

final class ClasStudentCache(colls: ClasColls, cacheApi: CacheApi)(using
    ec: ExecutionContext,
    scheduler: Scheduler,
    mat: Materializer
):

  private val falsePositiveRate = 0.00003
  private var bloomFilter       = BloomFilter[User.ID](100, falsePositiveRate) // temporary empty filter

  def isStudent(userId: User.ID) = bloomFilter mightContain userId

  def addStudent(userId: User.ID): Unit = bloomFilter add userId

  private def rebuildBloomFilter(): Unit =
    colls.student.countAll foreach { count =>
      val nextBloom = BloomFilter[User.ID](count + 1, falsePositiveRate)
      colls.student
        .find($doc("archived" $exists false), $doc("userId" -> true, "_id" -> false).some)
        .cursor[Bdoc](temporarilyPrimary)
        .documentSource()
        .throttle(300, 1.second)
        .toMat(Sink.fold[Int, Bdoc](0) { case (counter, doc) =>
          if (counter % 500 == 0) logger.info(s"ClasStudentCache.rebuild $counter")
          doc string "userId" foreach nextBloom.add
          counter + 1
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

  scheduler.scheduleWithFixedDelay(23 seconds, 3 hours) { (() => rebuildBloomFilter()) }.unit
