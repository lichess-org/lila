package lila.user

import org.joda.time.DateTime

case class Plan(
    months: Int,
    active: Boolean,
    since: Option[DateTime]
) {

  def incMonths = copy(
    months = months + 1,
    active = true,
    since = since orElse DateTime.now.some
  )

  def disable = copy(active = false)

  def enable = copy(
    active = true,
    since = since orElse DateTime.now.some
  )

  def isEmpty = months == 0

  def nonEmpty = !isEmpty option this

  def sinceDate = since | DateTime.now
}

object Plan {

  val empty = Plan(0, false, none)
  def start = empty.incMonths

  import lila.db.dsl._
  private[user] val planBSONHandler = reactivemongo.bson.Macros.handler[Plan]
}
