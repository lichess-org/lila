package lila.pool

import lila.user.User

case class Pool(
    setup: PoolSetup,
    users: List[User]) {

  lazy val sortedUsers = users.sortBy(u => -setup.glickoLens(u).intRating)

  lazy val rankedUsers = sortedUsers.zipWithIndex map {
    case (user, rank) => user -> (rank + 1)
  }

  lazy val nbUsers = users.size

  def contains(u: User) = users contains u

  def withUser(u: User) = copy(users = u :: users).distinctUsers

  private def distinctUsers = copy(
    users = users.map { u =>
      u.id -> u
    }.toMap.values.toList
  )

  def withoutUser(u: User) = copy(users = users filter (_.id != u.id))
}
