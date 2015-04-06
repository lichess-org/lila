package lila.simul

// All durations are expressed in seconds
case class SimulClock(
    limit: Int,
    increment: Int,
    hostExtraTime: Int) {

  def limitInMinutes = limit / 60

  def show = s"${limitInMinutes}+${increment}"

  def chessClock = chess.Clock(limit, increment)
    .giveTime(chess.White, hostExtraTime)

  def hostExtraMinutes = hostExtraTime / 60
}
