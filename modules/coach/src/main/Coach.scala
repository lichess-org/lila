package lila.coach

import lila.memo.PicfitImage

case class Coach(
    _id: Coach.Id, // user ID
    listed: Coach.Listed,
    available: Coach.Available,
    profile: CoachProfile,
    picture: Option[PicfitImage.Id],
    nbReviews: Int,
    user: Coach.User,
    languages: List[String],
    createdAt: Instant,
    updatedAt: Instant
):

  inline def id = _id

  def hasPicture = picture.isDefined

  def daysOld = daysBetween(createdAt, nowInstant)

  def withUser(user: lila.user.User.WithPerfs) = Coach.WithUser(this, user)

object Coach:

  opaque type Id = String
  object Id extends OpaqueUserId[Id]

  given UserIdOf[Coach] = _.id.userId

  val imageSize = 350

  def make(user: lila.user.User.WithPerfs) =
    Coach(
      _id = user.id into Id,
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

  case class WithUser(coach: Coach, user: lila.user.User.WithPerfs):
    def isListed = coach.listed.yes && user.enabled.yes && user.marks.clean

  opaque type Listed = Boolean
  object Listed extends YesNo[Listed]

  opaque type Available = Boolean
  object Available extends YesNo[Available]

  case class User(rating: IntRating, seenAt: Instant)
