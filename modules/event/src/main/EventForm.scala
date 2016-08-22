package lila.event

import org.joda.time.format.DateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import scala.util.Try

import lila.common.Form._

object EventForm {

  private val dateTimePattern = "yyyy-MM-dd HH:mm"

  private implicit val dateTimeFormat = format.Formats.jodaDateTimeFormat(dateTimePattern)

  val form = Form(mapping(
    "title" -> nonEmptyText(minLength = 3, maxLength = 40),
    "headline" -> nonEmptyText(minLength = 5, maxLength = 30),
    "homepageHours" -> number(min = 0, max = 24),
    "url" -> nonEmptyText,
    "startsAt" -> jodaDate(dateTimePattern),
    "finishesAt" -> jodaDate(dateTimePattern)
  )(Data.apply)(Data.unapply))

  case class Data(
      title: String,
      headline: String,
      homepageHours: Int,
      url: String,
      startsAt: DateTime,
      finishesAt: DateTime) {

    def update(event: Event) = event.copy(
      title = title,
      headline = headline,
      homepageHours = homepageHours,
      url = url,
      startsAt = startsAt,
      finishesAt = finishesAt)

    def make(userId: String) = Event(
      _id = Event.makeId,
      title = title,
      headline = headline,
      homepageHours = homepageHours,
      url = url,
      startsAt = startsAt,
      finishesAt = finishesAt,
      createdBy = Event.UserId(userId),
      createdAt = DateTime.now)
  }

  object Data {

    def make(event: Event) = Data(
      title = event.title,
      headline = event.headline,
      homepageHours = event.homepageHours,
      url = event.url,
      startsAt = event.startsAt,
      finishesAt = event.finishesAt)
  }
}
