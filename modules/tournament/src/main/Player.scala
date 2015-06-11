package lila.tournament

import lila.game.PerfPicker
import lila.rating.Perf
import lila.user.{ User, Perfs }

import ornicar.scalalib.Random

private[tournament] case class Player(
    id: String, // random
    tourId: String,
    userId: String,
    rating: Int,
    provisional: Boolean,
    withdraw: Boolean = false,
    score: Int = 0,
    perf: Int = 0,
    magicScore: Int = 0,
    rank: Int = Int.MaxValue) {

  def active = !withdraw

  def is(userId: String): Boolean = id == userId
  def is(user: User): Boolean = is(user.id)
  def is(other: Player): Boolean = is(other.id)

  def doWithdraw = copy(withdraw = true)
  def unWithdraw = copy(withdraw = false)

  def recomputeMagicScore = copy(
    magicScore = (score * 1000000) + (perf * 1000) + rating)
}

private[tournament] object Player {

  private[tournament] def make(tourId: String, user: User, perfLens: Perfs => Perf): Player = new Player(
    id = Random nextStringUppercase 8,
    tourId = tourId,
    userId = user.id,
    rating = perfLens(user.perfs).intRating,
    provisional = perfLens(user.perfs).provisional)
}
