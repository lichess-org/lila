package lila.coach

import org.joda.time.DateTime

import lila.user.User

case class Coach(
    _id: Coach.Id, // user ID
    enabledByUser: Coach.Enabled,
    enabledByMod: Coach.Enabled,
    available: Coach.Available,
    hourlyRate: Option[Coach.Cents],
    profile: CoachProfile,
    createdAt: DateTime,
    updatedAt: DateTime) {

  def id = _id

  def is(user: User) = id.value == user.id
}

object Coach {

  def make(user: User) = Coach(
    _id = Id(user.id),
    enabledByUser = Enabled(false),
    enabledByMod = Enabled(false),
    available = Available(false),
    hourlyRate = None,
    profile = CoachProfile(),
    createdAt = DateTime.now,
    updatedAt = DateTime.now)

  case class WithUser(coach: Coach, user: User)

  case class Id(value: String) extends AnyVal with StringValue
  case class Enabled(value: Boolean) extends AnyVal
  case class Available(value: Boolean) extends AnyVal
  case class Cents(value: Int) extends AnyVal
}
