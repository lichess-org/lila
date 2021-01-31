package lila.coach

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.Lang
import play.api.libs.json.{ JsSuccess, Json }

object CoachProfileForm {

  def edit(coach: Coach) =
    Form(
      mapping(
        "listed"    -> boolean,
        "available" -> boolean,
        "languages" -> nonEmptyText,
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
      languages = "",
      profile = coach.profile
    )

  private case class TagifyLang(code: String)
  implicit private val TagifyLangReads = Json.reads[TagifyLang]

  case class Data(
      listed: Boolean,
      available: Boolean,
      languages: String,
      profile: CoachProfile
  ) {

    def apply(coach: Coach) =
      coach.copy(
        listed = Coach.Listed(listed),
        available = Coach.Available(available),
        profile = profile,
        languages = Json.parse(languages).validate[List[TagifyLang]] match {
          case JsSuccess(langs, _) => langs.take(10).map(_.code).flatMap(Lang.get).map(_.code).distinct
          case _                   => Nil
        },
        updatedAt = DateTime.now
      )
  }

  import CoachProfile.RichText

  implicit private val richTextFormat =
    lila.common.Form.formatter.stringFormatter[RichText](_.value, RichText.apply)
  private def richText = of[RichText]
}
