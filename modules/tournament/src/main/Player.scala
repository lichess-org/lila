package lila.tournament

import lila.game.PerfPicker
import lila.rating.Perf
import lila.user.{ User, Perfs }

private[tournament] case class Player(
    _id: String, // random
    tourId: String,
    userId: String,
    rating: Int,
    provisional: Boolean,
    withdraw: Boolean = false,
    score: Int = 0,
    ratingDiff: Int = 0,
    magicScore: Int = 0,
    fire: Boolean = false,
    performance: Option[Int] = none) {

  def id = _id

  def active = !withdraw

  def is(uid: String): Boolean = uid == userId
  def is(user: User): Boolean = is(user.id)
  def is(other: Player): Boolean = is(other.userId)

  def doWithdraw = copy(withdraw = true)
  def unWithdraw = copy(withdraw = false)

  def finalRating = rating + ratingDiff

  def recomputeMagicScore = copy(
    magicScore = ((score * 100000) + (ratingDiff * 100) + (rating / 10)) atMost Int.MaxValue
  )
}

private[tournament] object Player {

  private[tournament] def make(tourId: String, user: User, perfLens: Perfs => Perf): Player = new Player(
    _id = lila.game.IdGenerator.game,
    tourId = tourId,
    userId = user.id,
    rating = perfLens(user.perfs).intRating,
    provisional = perfLens(user.perfs).provisional
  ).recomputeMagicScore
}
