package lila.clas

import akka.stream.Materializer
import akka.stream.scaladsl.*
import bloomfilter.mutable.BloomFilter
import reactivemongo.akkastream.cursorProducer
import play.api.Mode

import lila.db.dsl.{ *, given }

final class ClasUserFilters(using Executor, Materializer, Scheduler)(colls: ClasColls)(using mode: Mode):

  private val loadImmediately = true && mode == Mode.Dev

  val student = ClasUserCache("student")(
    colls.student,
    selector = $doc("archived".$exists(false)),
    projection = $doc("userId" -> true, "_id" -> false),
    reader = _.getAsOpt[UserId]("userId").toList,
    initialDelay = if loadImmediately then 1.second else 81.seconds,
    perSecond = 10_000
  )
  val teacher = ClasUserCache("teacher")(
    colls.clas,
    selector = $doc("archived".$exists(false)),
    projection = $doc("teachers" -> true, "_id" -> false),
    reader = _.getAsOpt[List[UserId]]("teachers").orZero,
    initialDelay = if loadImmediately then 1.second else 53.seconds,
    perSecond = 2_000
  )

private final class ClasUserCache(name: String)(
    coll: Coll,
    selector: Bdoc,
    projection: Bdoc,
    reader: Bdoc => List[UserId],
    initialDelay: FiniteDuration,
    perSecond: Int
)(using scheduler: Scheduler)(using Executor, Materializer):

  private val falsePositiveRate = 0.00003
  // Stick to [String], it does unsafe operations that don't play well with opaque types
  private var bloomFilter: BloomFilter[String] =
    BloomFilter[String](100, falsePositiveRate) // temporary empty filter

  def apply(userId: UserId) = bloomFilter.mightContain(userId.value)

  def add(userId: UserId): Unit = bloomFilter.add(userId.value)

  private def rebuildBloomFilter(): Unit =
    coll.secondary
      .countSel(selector)
      .foreach: count =>
        val nextBloom = BloomFilter[String](count + 1, falsePositiveRate)
        coll
          .find(selector, projection.some)
          .cursor[Bdoc](ReadPref.sec)
          .documentSource()
          .throttle(perSecond, 1.second)
          .runWith:
            Sink.fold[Int, Bdoc](0): (counter, doc) =>
              if counter % perSecond == 0 then logger.info(s"ClasUserCache.$name.rebuild $counter")
              UserId.raw(reader(doc)).foreach(nextBloom.add)
              counter + 1
          .addEffect: nb =>
            lila.mon.clas.bloomFilter(name).count.update(nb)
            bloomFilter.dispose()
            bloomFilter = nextBloom
          .monSuccess(_.clas.bloomFilter(name).fu)

  scheduler.scheduleWithFixedDelay(initialDelay, 7.days): () =>
    rebuildBloomFilter()
