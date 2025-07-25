package lila.insight

case class DateRange(min: Instant, max: Instant)

case class Period(days: Int):
  def max = nowInstant
  def min = max.minusDays(days)

  override def toString =
    days match
      case 1 => "Last 24 hours"
      case d if d < 14 => s"Last $d days"
      case d if d == 14 => s"Last week"
      case d if d < 30 => s"Last ${d / 7} weeks"
      case d if d == 30 => s"Last month"
      case d if d < 365 => s"Last ${d / 30} months"
      case d if d == 365 => s"Last year"
      case d => s"Last ${d / 365} years"

object Period:

  val selector: List[Period] = List(
    1,
    2,
    7,
    15,
    30,
    60,
    182,
    365,
    365 * 2,
    365 * 3,
    365 * 5,
    365 * 10
  ).map(Period.apply)
