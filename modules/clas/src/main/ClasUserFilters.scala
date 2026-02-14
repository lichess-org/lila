package lila.clas

import akka.stream.Materializer
import akka.stream.scaladsl.*
import bloomfilter.mutable.BloomFilter
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.BSONNull
import play.api.Mode

import lila.db.dsl.{ *, given }

final class ClasUserFilters(using Executor, Materializer, Scheduler)(colls: ClasColls)(using mode: Mode):

  private val loadImmediately = false && mode == Mode.Dev

  val student = ClasUserCache("student")(
    estimatedCount = if mode == Mode.Dev then 1_000 else 100_000,
    source = colls.clas
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match("archived".$exists(false)),
          PipelineOperator:
            $lookup.simple(
              from = colls.student,
              as = "s",
              local = "_id",
              foreign = "clasId",
              pipe = List(
                $doc("$match" -> $doc("archived".$exists(false))),
                $doc("$project" -> $doc("_id" -> false, "userId" -> true))
              )
            )
          ,
          Project($doc("_id" -> false, "s.userId" -> true)),
          UnwindField("s"),
          Group(BSONNull)("u" -> AddFieldToSet("s.userId")),
          UnwindField("u"),
          Project($doc("_id" -> false, "u" -> true))
        )
      .documentSource()
      .mapConcat(_.getAsOpt[UserId]("u").toList)
      .throttle(5_000, 1.second),
    initialDelay = if loadImmediately then 1.second else 41.seconds
  )

  val teacher = ClasUserCache("teacher")(
    estimatedCount = if mode == Mode.Dev then 50 else 8_000,
    source = colls.clas
      .find($doc("archived".$exists(false)), $doc("teachers" -> true, "_id" -> false).some)
      .cursor[Bdoc](ReadPref.sec)
      .documentSource()
      .mapConcat(_.getAsOpt[List[UserId]]("teachers").orZero)
      .throttle(2_000, 1.second),
    initialDelay = if loadImmediately then 1.second else 33.seconds
  )

private final class ClasUserCache(name: String)(
    estimatedCount: Int,
    source: Source[UserId, ?],
    initialDelay: FiniteDuration
)(using scheduler: Scheduler)(using Executor, Materializer):

  private val falsePositiveRate = 0.00003
  // Stick to [String], it does unsafe operations that don't play well with opaque types
  private var bloomFilter: BloomFilter[String] =
    BloomFilter[String](100, falsePositiveRate) // temporary empty filter

  def apply(userId: UserId) = bloomFilter.mightContain(userId.value)

  def add(userId: UserId): Unit = bloomFilter.add(userId.value)

  private def rebuildBloomFilter(): Unit =
    val nextBloom = BloomFilter[String](estimatedCount + 100, falsePositiveRate)
    def logNb(nb: Int) = logger.info(s"ClasUserCache.$name.rebuild $nb")
    source
      .runWith:
        Sink.fold[Int, UserId](0): (counter, userId) =>
          if counter % 10_000 == 0 then logNb(counter)
          nextBloom.add(userId.value)
          counter + 1
      .addEffect: nb =>
        logNb(nb)
        lila.mon.clas.bloomFilter(name).count.update(nb)
        bloomFilter.dispose()
        bloomFilter = nextBloom
      .monSuccess(_.clas.bloomFilter(name).fu)

  scheduler.scheduleWithFixedDelay(initialDelay, 7.days): () =>
    rebuildBloomFilter()
