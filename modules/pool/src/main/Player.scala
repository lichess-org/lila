package lila.pool

case class Player(
    user: lila.common.LightUser,
    rating: Int,
    pairable: Boolean) {

  def is(p: Player) = user.id == p.user.id

  def id = user.id

  def withRating(r: Int) = copy(rating = r)
}

object Player {

  case class Score(
    ratingPercent: Int,
    recentPairings: List[Pairing])
}
