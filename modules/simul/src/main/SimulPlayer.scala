package lila.simul

import chess.variant.Variant
import chess.IntRating
import chess.rating.RatingProvisional

import lila.core.user.WithPerf

private[simul] case class SimulPlayer(
    user: UserId,
    variant: Variant,
    rating: IntRating,
    provisional: Option[RatingProvisional]
):
  def is(userId: UserId): Boolean     = user == userId
  def is(other: SimulPlayer): Boolean = is(other.user)

private[simul] object SimulPlayer:

  private[simul] def make(user: WithPerf, variant: Variant): SimulPlayer =
    SimulPlayer(
      user = user.id,
      variant = variant,
      rating = user.perf.intRating,
      provisional = user.perf.provisional.some
    )
