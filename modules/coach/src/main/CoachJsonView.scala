package lila.coach

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.data.RichText

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
    list(c) ++ Json.toJsObject(ShownProfile.from(c.coach.profile))

private given OWrites[ShownProfile] = Json.writes

private case class ShownProfile(
    youtubeVideos: List[Url],
    studies: List[StudyId],
    description: Option[RichText],
    playingExperience: Option[RichText],
    teachingExperience: Option[RichText],
    otherExperience: Option[RichText],
    skills: Option[RichText],
    methodology: Option[RichText],
    youtubeChannel: Option[String]
)

private object ShownProfile:
  def from(p: CoachProfile): ShownProfile =
    ShownProfile(
      p.youtubeUrls,
      p.studyIds,
      p.description,
      p.playingExperience,
      p.teachingExperience,
      p.otherExperience,
      p.skills,
      p.methodology,
      p.youtubeChannel
    )
