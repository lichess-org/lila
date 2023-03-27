package lila.simul

import chess.{ Centis, Clock, Color }
import chess.Clock.LimitSeconds

case class SimulClock(
    config: Clock.Config,
    hostExtraTime: LimitSeconds,
    hostExtraTimePerPlayer: LimitSeconds
):

  def chessClockOf(hostColor: Color) =
    config.toClock.giveTime(
      hostColor,
      Centis.ofSeconds {
        hostExtraTime.value.atLeast(-config.limitSeconds.value + 20)
      }
    )

  def hostExtraMinutes          = hostExtraTime.value / 60
  def hostExtraMinutesPerPlayer = hostExtraTimePerPlayer.value / 60

  def adjustedForPlayers(numberOfPlayers: Int) =
    copy(hostExtraTime = hostExtraTime + numberOfPlayers * hostExtraTimePerPlayer.value)

  def valid =
    if (config.limitSeconds + hostExtraTime == LimitSeconds(0)) config.incrementSeconds >= 10
    else config.limitSeconds + hostExtraTime > 0
