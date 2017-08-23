package lila.simul

import chess.variant.Variant
import lila.user.{ User, Perfs }

private[simul] case class SimulPlayer(
    user: User.ID,
    variant: Variant,
    rating: Int,
    provisional: Option[Boolean]
) {

  def is(userId: User.ID): Boolean = user == userId
  def is(other: SimulPlayer): Boolean = is(other.user)
}

private[simul] object SimulPlayer {

  private[simul] def make(user: User, variant: Variant): SimulPlayer = {

    val perf =
      if (variant == chess.variant.Standard) {
        if (user.perfs.classical.nb >= 20 ||
          user.perfs.classical.nb > user.perfs.blitz.nb)
          user.perfs.classical
        else user.perfs.blitz
      } else Perfs.variantLens(variant).fold(user.perfs.standard)(_(user.perfs))

    new SimulPlayer(
      user = user.id,
      variant = variant,
      rating = perf.intRating,
      provisional = perf.provisional.some
    )
  }
}
