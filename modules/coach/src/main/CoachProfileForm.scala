package lila.coach

import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.{ JsSuccess, Json, Reads }

import lila.common.Form.given
import lila.core.data.RichText
import scalalib.model.LangTag

object CoachProfileForm:

  def edit(coach: Coach) =
    Form(
      mapping(
        "listed" -> boolean,
        "available" -> boolean,
        "languages" -> nonEmptyText,
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
    ).fill(
      Data(
        listed = coach.listed.value,
        available = coach.available.value,
        languages = "",
        profile = coach.profile
      )
    )

  private case class TagifyLang(code: String)
  private given Reads[TagifyLang] = Json.reads

  case class Data(
      listed: Boolean,
      available: Boolean,
      languages: String,
      profile: CoachProfile
  ):

    def apply(coach: Coach) =
      coach.copy(
        listed = Coach.Listed(listed),
        available = Coach.Available(available),
        profile = profile,
        languages = Json.parse(languages).validate[List[TagifyLang]] match
          case JsSuccess(langs, _) =>
            LangTag.from(langs.take(10).map(_.code)).flatMap(_.toLang).map(_.toTag).distinct
          case _ => Nil
        ,
        updatedAt = nowInstant
      )
