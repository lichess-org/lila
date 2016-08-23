package lila.coach

import org.joda.time.DateTime
import play.api.data._
import play.api.data.format.Formatter
import play.api.data.Forms._

object CoachForm {

  def edit(coach: Coach) = Form(mapping(
    "hourlyRate" -> optional(number(min = 5, max = 500)),
    "enabledByUser" -> boolean,
    "available" -> boolean,
    "profile" -> profileMapping
  )(Data.apply)(Data.unapply)) fill Data(
    hourlyRate = coach.hourlyRate.map(_.value),
    enabledByUser = coach.enabledByUser.value,
    available = coach.available.value,
    profile = coach.profile)

  case class Data(
      hourlyRate: Option[Int],
      enabledByUser: Boolean,
      available: Boolean,
      profile: CoachProfile) {

    def apply(coach: Coach) = coach.copy(
      hourlyRate = hourlyRate.map(_ * 100) map Coach.Cents.apply,
      enabledByUser = Coach.Enabled(enabledByUser),
      available = Coach.Available(available),
      profile = profile,
      updatedAt = DateTime.now)
  }

  private def profileMapping = mapping(
    "headline" -> optional(nonEmptyText(minLength = 5, maxLength = 140)),
    "description" -> optional(markdown),
    "playingExperience" -> optional(markdown),
    "teachingExperience" -> optional(markdown),
    "otherExperience" -> optional(markdown),
    "skills" -> optional(markdown),
    "methodology" -> optional(markdown)
  )(CoachProfile.apply)(CoachProfile.unapply)

  import CoachProfile.Markdown

  private def markdown = of[Markdown]
  private implicit val markdownFormat = lila.common.Form.formatter.stringFormatter[Markdown](_.value, Markdown.apply)
}
