package lidraughts.tournament

import lidraughts.rating.Perf
import lidraughts.user.{ User, Perfs }

private[tournament] case class Player(
    _id: String, // random
    tourId: Tournament.ID,
    userId: User.ID,
    rating: Int,
    provisional: Boolean,
    withdraw: Boolean = false,
    score: Int = 0,
    ratingDiff: Int = 0,
    fire: Boolean = false,
    performance: Int = 0
) {

  def id = _id

  def active = !withdraw

  def is(uid: String): Boolean = uid == userId
  def is(user: User): Boolean = is(user.id)
  def is(other: Player): Boolean = is(other.userId)

  def doWithdraw = copy(withdraw = true)
  def unWithdraw = copy(withdraw = false)

  def magicScore = score * 10000 + (performanceOption | rating)

  def performanceOption = performance > 0 option performance
}

private[tournament] object Player {

  case class WithUser(player: Player, user: User)

  private[tournament] def make(tourId: String, user: User, perfLens: Perfs => Perf): Player = new Player(
    _id = lidraughts.game.IdGenerator.game,
    tourId = tourId,
    userId = user.id,
    rating = perfLens(user.perfs).intRating,
    provisional = perfLens(user.perfs).provisional
  )
}
