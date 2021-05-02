package lila.common

import akka.NotUsed
import akka.stream.scaladsl._
import com.github.blemale.scaffeine.Scaffeine
import scala.annotation.nowarn
import scala.concurrent.duration._

object LilaStream {

  def flowRate[T](
      metric: T => Int = (_: T) => 1,
      outputDelay: FiniteDuration = 1 second
  ): Flow[T, Double, NotUsed] =
    Flow[T]
      .conflateWithSeed(metric(_)) { case (acc, x) => acc + metric(x) }
      .zip(Source.tick(outputDelay, outputDelay, NotUsed))
      .map(_._1.toDouble / outputDelay.toUnit(SECONDS))

  def logRate[T](
      name: String,
      metric: T => Int = (_: T) => 1,
      outputDelay: FiniteDuration = 1 second
  )(logger: play.api.LoggerLike): Flow[T, T, NotUsed] =
    Flow[T]
      .alsoTo(
        flowRate[T](metric, outputDelay)
          .to(Sink.foreach(r => logger.info(s"[rate] $name ${r.toInt}")))
      )

  val sinkCount = Sink.fold[Int, Any](0) { case (total, _) =>
    total + 1
  }

  def collect[A] =
    Flow[Option[A]] collect { case Some(a) =>
      a
    }
}
