package lila.user

import org.joda.time.DateTime

case class Plan(months: Int, active: Boolean) {

  def incMonths = copy(
    months = months + 1,
    active = true)

  def disable = copy(active = false)

  def enable = copy(
    active = true,
    months = months max 1)

  def isEmpty = months == 0

  def nonEmpty = !isEmpty option this
}

object Plan {

  val empty = Plan(0, false)
  val start = Plan(1, true)

  import lila.db.dsl._
  private[user] val planBSONHandler = reactivemongo.bson.Macros.handler[Plan]
}
