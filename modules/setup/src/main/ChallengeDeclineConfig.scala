package lila.setup

final case class ChallengeDeclineConfig(
    reason: Option[String] = None
) {
  def >> = (reason).some
}

object ChallengeDeclineConfig {
  def from(r: Option[String]) =
    new ChallengeDeclineConfig(
      reason = r
    )
}
