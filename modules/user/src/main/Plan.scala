package lila.user

case class Plan(
    months: Int,
    active: Boolean,
    since: Option[Instant]
):

  def incMonths =
    copy(
      months = months + 1,
      active = true,
      since = since orElse nowInstant.some
    )

  def disable = copy(active = false)

  def enable =
    copy(
      active = true,
      months = months atLeast 1,
      since = since orElse nowInstant.some
    )

  def isEmpty = months == 0

  def nonEmpty = !isEmpty option this

  def sinceDate = since | nowInstant

object Plan:

  val empty = Plan(0, active = false, none)
  def start = Plan(1, active = true, nowInstant.some)

  import lila.db.dsl.given
  import reactivemongo.api.bson.*
  private[user] given BSONDocumentHandler[Plan] = Macros.handler[Plan]
