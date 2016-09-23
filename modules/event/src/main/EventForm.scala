package lila.event

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import scala.util.Try

import lila.common.Form._

object EventForm {

  import lila.common.Form.UTCDate._

  val form = Form(mapping(
    "title" -> nonEmptyText(minLength = 3, maxLength = 40),
    "headline" -> nonEmptyText(minLength = 5, maxLength = 30),
    "description" -> optional(nonEmptyText(minLength = 5, maxLength = 4000)),
    "homepageHours" -> number(min = 0, max = 24),
    "url" -> nonEmptyText,
    "enabled" -> boolean,
    "startsAt" -> utcDate,
    "finishesAt" -> utcDate
  )(Data.apply)(Data.unapply))

  case class Data(
      title: String,
      headline: String,
      description: Option[String],
      homepageHours: Int,
      url: String,
      enabled: Boolean,
      startsAt: DateTime,
      finishesAt: DateTime) {

    def update(event: Event) = event.copy(
      title = title,
      headline = headline,
      description = description,
      homepageHours = homepageHours,
      url = url,
      enabled = enabled,
      startsAt = startsAt,
      finishesAt = finishesAt)

    def make(userId: String) = Event(
      _id = Event.makeId,
      title = title,
      headline = headline,
      description = description,
      homepageHours = homepageHours,
      url = url,
      enabled = enabled,
      startsAt = startsAt,
      finishesAt = finishesAt,
      createdBy = Event.UserId(userId),
      createdAt = DateTime.now)
  }

  object Data {

    def make(event: Event) = Data(
      title = event.title,
      headline = event.headline,
      description = event.description,
      homepageHours = event.homepageHours,
      url = event.url,
      enabled = event.enabled,
      startsAt = event.startsAt,
      finishesAt = event.finishesAt)
  }
}
