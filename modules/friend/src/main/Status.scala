package lila.friend

import lila.user.User

case class Status(
    user1: String,
    user2: String,
    friend: Option[Friend],
    request: Option[Request]) {

  def areFriends = friend.isDefined

  def requestBy(userId: String) = request ?? (_.user == userId)

  def not(userId: String) = (user1 == userId) ? user2 | user1
}

object Status {

  def apply(friend: Friend): Status = Status(friend.user1, friend.user2, friend.some, none)

  def apply(request: Request): Status = Status(request.user, request.friend, none, request.some)

  def apply(u1: String, u2: String): Status = Status(u1, u2, none, none)
}
