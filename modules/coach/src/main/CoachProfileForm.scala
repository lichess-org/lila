package lila.coach

import org.joda.time.DateTime
import play.api.i18n.Lang
import play.api.data._
import play.api.data.Forms._

import lila.common.Form.numberIn
import lila.i18n.LangList

object CoachProfileForm {

  def edit(coach: Coach) =
    Form(
      mapping(
        "listed"    -> boolean,
        "available" -> boolean,
        "languages" -> list(numberIn(Coach.Proficiency.range.toList)),
        "profile" -> mapping(
          "headline"           -> optional(text(minLength = 5, maxLength = 170)),
          "languages"          -> optional(text(minLength = 3, maxLength = 140)),
          "hourlyRate"         -> optional(text(minLength = 3, maxLength = 140)),
          "description"        -> optional(richText),
          "playingExperience"  -> optional(richText),
          "teachingExperience" -> optional(richText),
          "otherExperience"    -> optional(richText),
          "skills"             -> optional(richText),
          "methodology"        -> optional(richText),
          "youtubeVideos"      -> optional(nonEmptyText),
          "youtubeChannel"     -> optional(nonEmptyText),
          "publicStudies"      -> optional(nonEmptyText)
        )(CoachProfile.apply)(CoachProfile.unapply)
      )(Data.apply)(Data.unapply)
    ) fill Data(
      listed = coach.listed.value,
      available = coach.available.value,
      languages = LangList.popular.map { l =>
        coach.languages.flatMap(_ get l).??(_.value)
      },
      profile = coach.profile
    )

  case class Data(
      listed: Boolean,
      available: Boolean,
      languages: List[Int],
      profile: CoachProfile
  ) {

    def apply(coach: Coach) = coach.copy(
      listed = Coach.Listed(listed),
      available = Coach.Available(available),
      profile = profile,
      languages = languagesMap
        .filter(_._2.value > 0)
        .some
        .filter(_.nonEmpty),
      updatedAt = DateTime.now
    )

    lazy val proficiencies: List[(Lang, Coach.Proficiency)] =
      LangList.popular.zip(languages map Coach.Proficiency.apply)

    lazy val languagesMap: Coach.Languages = proficiencies.toMap
  }

  import CoachProfile.RichText

  implicit private val richTextFormat =
    lila.common.Form.formatter.stringFormatter[RichText](_.value, RichText.apply)
  private def richText = of[RichText]
}
