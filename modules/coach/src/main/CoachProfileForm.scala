package lila.coach

import org.joda.time.DateTime
import play.api.data._
import play.api.data.format.Formatter
import play.api.data.Forms._

object CoachProfileForm {

  def edit(coach: Coach) = Form(mapping(
    "enabledByUser" -> boolean,
    "available" -> boolean,
    "profile" -> profileMapping
  )(Data.apply)(Data.unapply)) fill Data(
    enabledByUser = coach.enabledByUser.value,
    available = coach.available.value,
    profile = coach.profile)

  case class Data(
      enabledByUser: Boolean,
      available: Boolean,
      profile: CoachProfile) {

    def apply(coach: Coach) = coach.copy(
      enabledByUser = Coach.Enabled(enabledByUser),
      available = Coach.Available(available),
      profile = profile,
      updatedAt = DateTime.now)
  }

  private def profileMapping = mapping(
    "headline" -> optional(nonEmptyText(minLength = 5, maxLength = 170)),
    "languages" -> optional(nonEmptyText(minLength = 3, maxLength = 140)),
    "hourlyRate" -> optional(nonEmptyText(minLength = 3, maxLength = 140)),
    "description" -> optional(richText),
    "playingExperience" -> optional(richText),
    "teachingExperience" -> optional(richText),
    "otherExperience" -> optional(richText),
    "skills" -> optional(richText),
    "methodology" -> optional(richText),
    "youtubeVideos" -> optional(nonEmptyText),
    "youtubeChannel" -> optional(nonEmptyText),
    "publicStudies" -> optional(nonEmptyText)
  )(CoachProfile.apply)(CoachProfile.unapply)

  import CoachProfile.RichText

  private def richText = of[RichText]
  private implicit val richTextFormat = lila.common.Form.formatter.stringFormatter[RichText](_.value, RichText.apply)
}
