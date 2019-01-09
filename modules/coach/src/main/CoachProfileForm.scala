package lila.coach

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

object CoachProfileForm {

  def edit(coach: Coach) = Form(mapping(
    "listed" -> boolean,
    "available" -> boolean,
    "profile" -> mapping(
      "headline" -> optional(text(minLength = 5, maxLength = 170)),
      "languages" -> optional(text(minLength = 3, maxLength = 140)),
      "hourlyRate" -> optional(text(minLength = 3, maxLength = 140)),
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
  )(Data.apply)(Data.unapply)) fill Data(
    listed = coach.listed.value,
    available = coach.available.value,
    profile = coach.profile
  )

  case class Data(
      listed: Boolean,
      available: Boolean,
      profile: CoachProfile
  ) {

    def apply(coach: Coach) = coach.copy(
      listed = Coach.Listed(listed),
      available = Coach.Available(available),
      profile = profile,
      updatedAt = DateTime.now
    )
  }

  import CoachProfile.RichText

  private implicit val richTextFormat = lila.common.Form.formatter.stringFormatter[RichText](_.value, RichText.apply)
  private def richText = of[RichText]
}
