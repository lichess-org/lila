package lila.coach

import org.joda.time.{ DateTime, Days }

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
    createdAt: DateTime,
    updatedAt: DateTime
) {

  def id = _id

  def is(user: lila.user.User) = id.value == user.id

  def hasPicture = picture.isDefined

  def daysOld = Days.daysBetween(createdAt, DateTime.now).getDays
}

object Coach {

  def make(user: lila.user.User) =
    Coach(
      _id = Id(user.id),
      listed = Listed(false),
      available = Available(true),
      profile = CoachProfile(),
      picture = None,
      nbReviews = 0,
      user = User(user.perfs.bestStandardRating, user.seenAt | user.createdAt),
      languages = user.lang.toList,
      createdAt = DateTime.now,
      updatedAt = DateTime.now
    )

  case class WithUser(coach: Coach, user: lila.user.User) {
    def isListed = coach.listed.value && user.enabled && user.marks.clean
  }

  case class Id(value: String)          extends AnyVal with StringValue
  case class Listed(value: Boolean)     extends AnyVal
  case class Available(value: Boolean)  extends AnyVal
  case class PicturePath(value: String) extends AnyVal with StringValue
  case class User(rating: Int, seenAt: DateTime)
}
