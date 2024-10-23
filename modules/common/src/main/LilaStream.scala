package lila.common

import akka.NotUsed
import akka.stream.scaladsl.*

object LilaStream:

  def flowRate[T](
      metric: T => Int = (_: T) => 1,
      outputDelay: FiniteDuration = 1.second
  ): Flow[T, Double, NotUsed] =
    Flow[T]
      .conflateWithSeed(metric(_)) { (acc, x) => acc + metric(x) }
      .zip(Source.tick(outputDelay, outputDelay, NotUsed))
      .map(_._1.toDouble / outputDelay.toUnit(concurrent.duration.SECONDS))

  def logRate[T](
      name: String,
      metric: T => Int = (_: T) => 1,
      outputDelay: FiniteDuration = 1.second
  )(logger: play.api.LoggerLike): Flow[T, T, NotUsed] =
    Flow[T].alsoTo:
      flowRate[T](metric, outputDelay)
        .to(Sink.foreach(r => logger.info(s"[rate] $name ${r.toInt}")))

  val sinkCount: Sink[Any, Future[Int]] = Sink.fold[Int, Any](0): (total, _) =>
    total + 1

  val sinkSum: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0): (total, nb) =>
    total + nb

  def collect[A]: Flow[Option[A], A, NotUsed] = Flow[Option[A]].collect:
    case Some(a) => a
