package lila.user

import org.joda.time.DateTime

case class Plan(
    months: Int,
    activeUntil: Option[DateTime],
    customerId: String) {

  def active = activeUntil ?? DateTime.now.isBefore

  def level = months + 1
}

object Plan {

  import lila.db.dsl._
  private[user] val planBSONHandler = reactivemongo.bson.Macros.handler[Plan]
}
