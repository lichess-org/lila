package lila.coach

import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ tagifyValues, given }
import lila.core.data.RichText
import scalalib.model.LangTag

object CoachProfileForm:

  def edit(coach: Coach) =
    Form(
      mapping(
        "listed" -> boolean,
        "available" -> boolean,
        "languages" -> tagifyValues.field[String, List[LangTag]]("code"): codes =>
          LangTag.from(codes.take(10)).flatMap(_.toLang).map(_.toTag),
        "profile" -> mapping(
          "headline" -> optional(text(minLength = 5, maxLength = 170)),
          "hourlyRate" -> optional(text(minLength = 3, maxLength = 140)),
          "description" -> optional(of[RichText]),
          "playingExperience" -> optional(of[RichText]),
          "teachingExperience" -> optional(of[RichText]),
          "otherExperience" -> optional(of[RichText]),
          "skills" -> optional(of[RichText]),
          "methodology" -> optional(of[RichText]),
          "youtubeVideos" -> optional(nonEmptyText),
          "youtubeChannel" -> optional(nonEmptyText),
          "publicStudies" -> optional(nonEmptyText)
        )(CoachProfile.apply)(unapply)
      )(Data.apply)(unapply)
    ).fill:
      Data(
        listed = coach.listed.value,
        available = coach.available.value,
        languages = Nil,
        profile = coach.profile
      )

  case class Data(
      listed: Boolean,
      available: Boolean,
      languages: List[LangTag],
      profile: CoachProfile
  ):

    def apply(coach: Coach) =
      coach.copy(
        listed = Coach.Listed(listed),
        available = Coach.Available(available),
        profile = profile,
        languages = languages,
        updatedAt = nowInstant
      )
