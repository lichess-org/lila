package lila.simul

import lila.rating.Perf
import lila.user.{ User, Perfs}
import lila.game.PerfPicker

private[simul] case class SimulPlayer(
    id: String,
    rating: Int) {

  def is(userId: String): Boolean = id == userId
  def is(user: User): Boolean = is(user.id)
  def is(other: SimulPlayer): Boolean = is(other.id)
}

private[simul] object SimulPlayer {

  private[simul] def make(user: User, perfLens: Perfs => Perf): SimulPlayer = new SimulPlayer(
    id = user.id,
    rating = perfLens(user.perfs).intRating)
}
