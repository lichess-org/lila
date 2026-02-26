package lila.tutor

import java.time.{ LocalDate, LocalTime }
import java.time.format.DateTimeFormatter
import play.api.mvc.Call
import lila.common.LilaOpeningFamily

case class TutorConfig(user: UserId, from: Instant, to: Instant):

  val rangeStr = s"${TutorConfig.format(from.date)}_${TutorConfig.format(to.date)}"
  val id = s"$user:$rangeStr"

  def period = (from, to)
  lazy val days = daysBetween(from, to)

  object url:
    def root = routes.Tutor.report(user, rangeStr)
    def perf(pk: PerfKey) = routes.Tutor.perf(user, rangeStr, pk)
    def angle(pk: PerfKey, a: Angle): Call = routes.Tutor.angle(user, rangeStr, pk, a)
    def angle(pk: PerfKey, a: Option[Angle]): Call = a.fold(perf(pk))(angle(pk, _))
    def opening(pk: PerfKey, color: Color, opening: LilaOpeningFamily): Call =
      routes.Tutor.opening(user, rangeStr, pk, color, opening.key.value)

object TutorConfig:

  def parse(user: UserId, urlFragment: String): Option[TutorConfig] =
    urlFragment.split("_") match
      case Array(fromStr, toStr) =>
        for
          from <- parseDate(fromStr)
          to <- parseDate(toStr)
        yield TutorConfig(user, fromDate(from), toDate(to))
      case _ => none

  private def parseDate(str: String): Option[LocalDate] =
    scala.util.Try(LocalDate.parse(str, dateFormatter)).toOption

  private def fromDate(date: LocalDate) = date.atStartOfDay.instant
  private def toDate(date: LocalDate) = date.atTime(maxLocalTime).instant
  private val maxLocalTime = LocalTime.of(23, 59, 59, 999_000_000) // ms precision

  val minFrom = lila.insight.minDate.date

  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  def format(date: LocalDate) = date.format(dateFormatter)

  object form:

    import play.api.data.Form
    import play.api.data.Forms.*
    import lila.common.Form.ISODate

    case class LocalDates(from: LocalDate, to: LocalDate):
      def config(user: UserId) = TutorConfig(user, fromDate(from), toDate(to))

    def from(config: TutorConfig) = LocalDates(config.from.date, config.to.date)

    def dates = Form:
      mapping(
        "from" -> ISODate.mapping.verifying(
          s"From date must be after ${format(minFrom)}",
          _.isAfter(minFrom.minusDays(1))
        ),
        "to" -> ISODate.mapping.verifying(
          "Date cannot be in the future",
          _.isBefore(LocalDate.now.plusDays(1))
        )
      )(LocalDates.apply)(lila.common.extensions.unapply)
        .verifying(
          "From date must be before to date",
          config => config.from.isBefore(config.to)
        )

    def full = dates.fill(LocalDates(minFrom, LocalDate.now))

    def default = dates.fill(LocalDates(LocalDate.now.minusMonths(6), LocalDate.now))
