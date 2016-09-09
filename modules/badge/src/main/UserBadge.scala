package lila.badge

import org.joda.time.DateTime

import lila.user.User

case class UserBadge(
  _id: String, // user:badge
  user: User.ID,
  badge: Badge,
  date: DateTime)

object UserBadge {

  def makeId(user: User, badge: Badge) = s"${user.id}:${badge.id}"

  def make(user: User, badge: Badge) = UserBadge(
    _id = makeId(user, badge),
    user = user.id,
    badge = badge,
    date = DateTime.now)
}
