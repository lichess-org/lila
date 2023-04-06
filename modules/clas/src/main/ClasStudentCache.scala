package lila.clas

import akka.stream.Materializer
import akka.stream.scaladsl.*
import bloomfilter.mutable.BloomFilter
import reactivemongo.akkastream.cursorProducer

import lila.db.dsl.{ *, given }

final class ClasStudentCache(colls: ClasColls)(using
    ec: Executor,
    scheduler: Scheduler,
    mat: Materializer
):

  private val falsePositiveRate = 0.00003
  // Stick to [String], it does unsafe operation that don't play well with opaque types
  private var bloomFilter: BloomFilter[String] =
    BloomFilter[String](100, falsePositiveRate) // temporary empty filter

  def isStudent(userId: UserId) = bloomFilter mightContain userId.value

  def addStudent(userId: UserId): Unit = bloomFilter add userId.value

  private def rebuildBloomFilter(): Unit =
    colls.student.countAll foreach { count =>
      val nextBloom = BloomFilter[String](count + 1, falsePositiveRate)
      colls.student
        .find($doc("archived" $exists false), $doc("userId" -> true, "_id" -> false).some)
        .cursor[Bdoc](temporarilyPrimary)
        .documentSource()
        .throttle(300, 1.second)
        .toMat(Sink.fold[Int, Bdoc](0) { case (counter, doc) =>
          if (counter % 500 == 0) logger.info(s"ClasStudentCache.rebuild $counter")
          doc.string("userId") foreach nextBloom.add
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

  scheduler.scheduleWithFixedDelay(71.seconds, 24.hours) { (() => rebuildBloomFilter()) }.unit
