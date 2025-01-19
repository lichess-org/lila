package lila.simul

import shogi.Centis
import shogi.Clock
import shogi.Color

// All durations are expressed in seconds
case class SimulClock(
    config: Clock.Config,
    hostExtraTime: Int,
) {

  def shogiClockOf(hostColor: Color) =
    config.toClock.giveTime(hostColor, Centis.ofSeconds(hostExtraTime))

  def hostExtraMinutes = hostExtraTime / 60
}
