package lila.user

case class Plan(
    months: Int,
    active: Boolean,
    since: Option[DateTime]
):

  def incMonths =
    copy(
      months = months + 1,
      active = true,
      since = since orElse nowDate.some
    )

  def disable = copy(active = false)

  def enable =
    copy(
      active = true,
      months = months atLeast 1,
      since = since orElse nowDate.some
    )

  def isEmpty = months == 0

  def nonEmpty = !isEmpty option this

  def sinceDate = since | nowDate

object Plan:

  val empty = Plan(0, active = false, none)
  def start = Plan(1, active = true, nowDate.some)

  import lila.db.dsl.{ *, given }
  import reactivemongo.api.bson.*
  private[user] given BSONDocumentHandler[Plan] = Macros.handler[Plan]
