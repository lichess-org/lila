package lila.simul

final case class SimulApplicant(
    player: SimulPlayer,
    accepted: Boolean
):

  def is(userId: UserId): Boolean     = player.is(userId)
  def is(other: SimulPlayer): Boolean = player.is(other)
