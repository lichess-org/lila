package lila.pool

import lila.rating.Glicko
import lila.user.User

case class PoolSetup(
    id: ID, // also a slug
    name: String,
    clockLimit: Int, // seconds
    clockIncrement: Int, // seconds
    variant: chess.Variant) {

  def glickoLens = (user: User) => user.perfs.global.glicko
}
