package lila
package implicits

import akka.util.Duration
import akka.util.duration._
import scala.util.Random
import scala.math.round
import ornicar.scalalib.Random.approximatly

object RichDuration {

  implicit def richDuration(d: Duration) = new {

    def randomize(ratio: Float = 0.1f): Duration =
      approximatly(0.1f)(d.toMillis) millis
  }
}
