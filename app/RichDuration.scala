package lila

import akka.util.Duration
import akka.util.duration._
import scala.util.Random
import scala.math.round

object RichDuration {

  implicit def richDuration(d: Duration) = new {

    def randomize(ratio: Float = 0.1f): Duration = {
      val m = d.toMillis
      val m2 = round(m + (ratio * m * 2 * Random.nextFloat) - (ratio * m))
      m2 millis
    }
  }
}
