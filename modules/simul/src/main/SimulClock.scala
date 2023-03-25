package lila.simul

import chess.{ Centis, Clock, Color }
import chess.Clock.LimitMinutes

// All durations are expressed in seconds
case class SimulClock(
    config: Clock.Config,
    hostExtraTime: Int,
    hostExtraTimePerPlayer: LimitMinutes
):

  def chessClockOf(hostColor: Color) =
    config.toClock.giveTime(
      hostColor,
      Centis.ofSeconds {
        hostExtraTime.atLeast(-config.limitSeconds.value + 20)
      }
    )

  def hostExtraMinutes = hostExtraTime / 60

  def adjustedForPlayers(numberOfPlayers: Int) =
    copy(hostExtraTime = hostExtraTime + numberOfPlayers * (LimitMinutes raw hostExtraTimePerPlayer) * 60)

  def valid =
    if (config.limitSeconds.value + hostExtraTime == 0) config.incrementSeconds >= 10
    else config.limitSeconds + hostExtraTime > 0
