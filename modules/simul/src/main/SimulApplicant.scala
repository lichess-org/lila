package lila.simul

final case class SimulApplicant(
    player: SimulPlayer,
    accepted: Boolean
) {

  def is(userId: String): Boolean     = player is userId
  def is(other: SimulPlayer): Boolean = player is other
}

private[simul] object SimulApplicant {

  def make(player: SimulPlayer): SimulApplicant =
    new SimulApplicant(
      player = player,
      accepted = false
    )
}
