package lila
package tournament

import org.joda.time.{ DateTime, Duration }
import org.scala_tools.time.Imports._
import com.novus.salat.annotations.Key
import ornicar.scalalib.OrnicarRandom

case class Tournament(
    @Key("_id") id: String = OrnicarRandom nextString 8,
    createdBy: String,
    startsAt: DateTime,
    minutes: Int,
    finished: Boolean = false,
    createdAt: DateTime = DateTime.now,
    users: List[String] = Nil) {

  lazy val duration = new Duration(minutes * 60 * 1000)

  lazy val endsAt = DateTime.now + duration
}

object Tournament {
  
  import lila.core.Form._

  val minutes = 5 to 30 by 5
  val minuteDefault = 10
  val minuteChoices = options(minutes, "%d minute{s}")
}
