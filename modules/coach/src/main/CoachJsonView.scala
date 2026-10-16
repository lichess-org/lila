package lila.coach

import play.api.libs.json.*

import lila.common.Json.given

final class CoachJsonView(picfitUrl: lila.memo.PicfitUrl):

  def list(c: Coach.WithUser): JsObject =
    Json
      .obj(
        "user" -> c.user.light,
        "rating" -> c.coach.user.rating,
        "languages" -> c.coach.languages.map(_.value),
        "nbReviews" -> c.coach.nbReviews,
        "available" -> c.coach.available.value,
        "listed" -> c.coach.listed.value
      )
      .add("headline", c.coach.profile.headline)
      .add("hourlyRate", c.coach.profile.hourlyRate)
      .add("picture", c.coach.picture.map(picfitUrl.thumbnail(_)(Coach.imageDimensions)))

  def show(c: Coach.WithUser): JsObject =
    val p = c.coach.profile
    list(c) ++ Json.obj(
      "youtubeVideos" -> p.youtubeUrls.map(_.value),
      "studies" -> p.studyIds.map(_.value)
    )
      .add("description", p.description.map(_.value))
      .add("playingExperience", p.playingExperience.map(_.value))
      .add("teachingExperience", p.teachingExperience.map(_.value))
      .add("otherExperience", p.otherExperience.map(_.value))
      .add("skills", p.skills.map(_.value))
      .add("methodology", p.methodology.map(_.value))
      .add("youtubeChannel", p.youtubeChannel)
