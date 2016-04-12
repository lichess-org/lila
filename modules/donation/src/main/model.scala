package lila.donation

import org.joda.time._

case class Progress(
    from: DateTime,
    goal: Int,
    current: Int) {

  val to = from plusWeeks 1

  val remainingDays = Days.daysBetween(DateTime.now.toLocalDate, to.toLocalDate).getDays()

  val percent = (current * 100) / goal

  val complete = goal >= current
}
