package lila.coach

import reactivemongo.api.bson.Macros.Annotations.Key
import chess.IntRating
import scalalib.model.LangTag

import lila.core.id.ImageId
import lila.core.perf.UserWithPerfs
import lila.rating.UserPerfsExt.bestStandardRating

case class Coach(
    @Key("_id") id: Coach.Id, // user ID
    listed: Coach.Listed,
    available: Coach.Available,
    profile: CoachProfile,
    picture: Option[ImageId],
    nbReviews: Int,
    user: Coach.User,
    languages: List[LangTag],
    createdAt: Instant,
    updatedAt: Instant
):
  def hasPicture = picture.isDefined

  def daysOld = daysBetween(createdAt, nowInstant)

  def withUser(user: UserWithPerfs) = Coach.WithUser(this, user)

object Coach:

  opaque type Id = String
  object Id extends lila.core.userId.OpaqueUserId[Id]

  given UserIdOf[Coach] = _.id.userId

  val imageSize = 350

  def make(user: UserWithPerfs) =
    Coach(
      id = user.id.into(Id),
      listed = Listed(false),
      available = Available(true),
      profile = CoachProfile(),
      picture = None,
      nbReviews = 0,
      user = User(user.perfs.bestStandardRating, user.seenAt | user.createdAt),
      languages = user.lang.toList,
      createdAt = nowInstant,
      updatedAt = nowInstant
    )

  case class WithUser(coach: Coach, user: UserWithPerfs):
    def isListed = coach.listed.yes && user.enabled.yes && user.marks.clean

  opaque type Listed = Boolean
  object Listed extends YesNo[Listed]

  opaque type Available = Boolean
  object Available extends YesNo[Available]

  case class User(rating: IntRating, seenAt: Instant)
