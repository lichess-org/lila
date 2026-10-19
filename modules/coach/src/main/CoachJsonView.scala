package lila.coach

import play.api.libs.json.*

import lila.common.Json.given

final class CoachJsonView(picfitUrl: lila.memo.PicfitUrl):

  def list(c: Coach.WithUser): JsObject =
    Json
      .obj(
        "user" -> c.user.light,
        "rating" -> c.coach.user.rating,
        "languages" -> c.coach.languages,
        "nbReviews" -> c.coach.nbReviews,
        "available" -> c.coach.available,
        "listed" -> c.coach.listed
      )
      .add("headline", c.coach.profile.headline)
      .add("hourlyRate", c.coach.profile.hourlyRate)
      .add("picture", c.coach.picture.map(picfitUrl.thumbnail(_)(Coach.imageDimensions)))

  def show(c: Coach.WithUser): JsObject =
    val p = c.coach.profile
    list(c) ++ Json
      .obj(
        "youtubeVideos" -> p.youtubeUrls,
        "studies" -> p.studyIds
      )
      .add("description", p.description)
      .add("playingExperience", p.playingExperience)
      .add("teachingExperience", p.teachingExperience)
      .add("otherExperience", p.otherExperience)
      .add("skills", p.skills)
      .add("methodology", p.methodology)
      .add("youtubeChannel", p.youtubeChannel)
