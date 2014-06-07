package lila.pool

import lila.user.User

case class Pool(
    setup: PoolSetup,
    users: List[User]) {

  def sortedUsers = users.sortBy(u => -setup.ratingLens(u).intRating)

  def nbUsers = users.size
}
