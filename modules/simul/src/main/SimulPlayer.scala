package lila.simul

import chess.variant.Variant
import lila.rating.Perf
import lila.user.User

private[simul] case class SimulPlayer(
    user: UserId,
    variant: Variant,
    rating: IntRating,
    provisional: Option[RatingProvisional]
):

  def is(userId: UserId): Boolean     = user == userId
  def is(other: SimulPlayer): Boolean = is(other.user)

private[simul] object SimulPlayer:

  private[simul] def make(user: User, variant: Variant, perf: Perf): SimulPlayer =
    new SimulPlayer(
      user = user.id,
      variant = variant,
      rating = perf.intRating,
      provisional = perf.provisional.some
    )
