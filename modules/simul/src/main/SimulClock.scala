package lila.simul

import chess.{ Centis, Clock, Color }

// All durations are expressed in seconds
case class SimulClock(
    config: Clock.Config,
    hostExtraTime: Int
):

  def chessClockOf(hostColor: Color) =
    config.toClock.giveTime(
      hostColor,
      Centis.ofSeconds {
        hostExtraTime.atLeast(-config.limitSeconds.value + 20)
      }
    )

  def hostExtraMinutes = hostExtraTime / 60

  def valid =
    if (config.limitSeconds.value + hostExtraTime == 0) config.incrementSeconds >= 10
    else config.limitSeconds + hostExtraTime > 0
