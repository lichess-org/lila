package lila.user

import org.joda.time.DateTime

case class Plan(months: Int, active: Boolean) {

  def incMonths = copy(
    months = months + 1,
    active = true)

  def disable = copy(active = false)
}

object Plan {

  val init = Plan(1, true)

  import lila.db.dsl._
  private[user] val planBSONHandler = reactivemongo.bson.Macros.handler[Plan]
}
