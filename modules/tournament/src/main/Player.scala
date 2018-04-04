package lila.tournament

import lila.rating.Perf
import lila.user.{ User, Perfs }

private[tournament] case class Player(
    _id: Player.ID, // random
    tourId: Tournament.ID,
    userId: User.ID,
    rating: Int,
    provisional: Boolean,
    withdraw: Boolean = false,
    score: Int = 0,
    fire: Boolean = false,
    performance: Int = 0
) {

  def id = _id

  def active = !withdraw

  def is(uid: User.ID): Boolean = uid == userId
  def is(user: User): Boolean = is(user.id)
  def is(other: Player): Boolean = is(other.userId)

  def doWithdraw = copy(withdraw = true)
  def unWithdraw = copy(withdraw = false)

  def magicScore = score * 10000 + (performanceOption | rating)

  def performanceOption = performance > 0 option performance
}

private[tournament] object Player {

  type ID = String

  case class WithUser(player: Player, user: User)

  private[tournament] def make(tourId: Tournament.ID, user: User, perfLens: Perfs => Perf): Player = new Player(
    _id = lila.game.IdGenerator.game,
    tourId = tourId,
    userId = user.id,
    rating = perfLens(user.perfs).intRating,
    provisional = perfLens(user.perfs).provisional
  )
}
