package lila.donation

import org.joda.time.DateTime

case class Progress(
    from: DateTime,
    goal: Int,
    current: Int) {

  def percent = (current * 100) / goal

  def complete = goal >= current
}
