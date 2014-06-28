package lila.pool

import lila.rating.Glicko
import lila.user.User

case class PoolSetup(
    id: ID, // also a slug
    name: String,
    clockLimit: Int, // seconds
    clockIncrement: Int, // seconds
    variant: chess.Variant) {

  val glickoLens = (user: User) => user.perfs.pool(id).glicko

  val clock = chess.Clock(clockLimit, clockIncrement)

  def playerOf(user: User) = Player(
    lila.common.LightUser(user.id, user.username, user.title),
    rating = glickoLens(user).intRating,
    waitingSince = none)


}
