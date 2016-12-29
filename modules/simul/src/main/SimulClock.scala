package lila.simul

// All durations are expressed in seconds
case class SimulClock(
    config: chess.Clock.Config,
    hostExtraTime: Int) {

  def chessClockOf(hostColor: chess.Color) =
    config.toClock.giveTime(hostColor, hostExtraTime)

  def hostExtraMinutes = hostExtraTime / 60
}
