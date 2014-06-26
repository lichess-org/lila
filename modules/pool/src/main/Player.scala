package lila.pool

import org.joda.time.{ DateTime, Seconds }

case class Player(
    user: lila.common.LightUser,
    rating: Int,
    waitingSince: Option[DateTime]) {

  def is(p: Player) = user.id == p.user.id

  def id = user.id

  def withRating(r: Int) = copy(rating = r)

  def waiting = waitingSince.isDefined

  def pairable = waitingSince ?? { date =>
    Seconds.secondsBetween(date, DateTime.now).getSeconds >= 2
  }

  def setWaiting(v: Boolean) = copy(
    waitingSince = v ?? { waitingSince orElse DateTime.now.some }
  )
}

object Player {

  case class Score(
    ratingPercent: Int,
    recentPairings: List[Pairing])
}
