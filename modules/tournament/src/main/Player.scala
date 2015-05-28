package lila.tournament

import lila.game.PerfPicker
import lila.rating.Perf
import lila.user.{ User, Perfs }

private[tournament] case class Player(
    id: String,
    rating: Int,
    withdraw: Boolean = false,
    score: Int = 0,
    perf: Int = 0) {

  def active = !withdraw

  def is(userId: String): Boolean = id == userId
  def is(user: User): Boolean = is(user.id)
  def is(other: Player): Boolean = is(other.id)

  def doWithdraw = copy(withdraw = true)
  def unWithdraw = copy(withdraw = false)

  def magicScore =
    (score * 1000000) + (perf * 1000) + rating + withdraw.fold(Int.MinValue / 2, 0)
}

private[tournament] object Player {

  private[tournament] def make(user: User, perfLens: Perfs => Perf): Player = new Player(
    id = user.id,
    rating = perfLens(user.perfs).intRating)

  private[tournament] def refresh(tour: Tournament): Players = tour.players map { p =>
    p.copy(
      score = tour.system.scoringSystem.scoreSheet(tour, p.id).total,
      perf = tour.pairings.foldLeft(0) {
        case (perf, pairing) => perf + pairing.perfOf(p.id)
      })
  } sortBy (-_.magicScore)
}
