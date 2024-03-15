package lila.simul

import chess.Clock.{ LimitMinutes, LimitSeconds }
import chess.{ Centis, Clock, Color }

case class SimulClock(
    config: Clock.Config,
    hostExtraTime: LimitSeconds,
    hostExtraTimePerPlayer: LimitSeconds
):

  def chessClockOf(hostColor: Color) =
    config.toClock.giveTime(
      hostColor,
      Centis.ofSeconds {
        hostExtraTime.atLeast(-config.limitSeconds + 20).value
      }
    )

  def hostExtraMinutes = LimitMinutes(hostExtraTime.value / 60)
  def hostExtraTimePerPlayerForDisplay: Option[Either[LimitMinutes, LimitSeconds]] =
    (hostExtraTimePerPlayer > 0).option(
      if hostExtraTimePerPlayer.value % 60 == 0 then Left(LimitMinutes(hostExtraTimePerPlayer.value / 60))
      else Right(LimitSeconds(hostExtraTimePerPlayer.value))
    )

  def adjustedForPlayers(numberOfPlayers: Int) =
    copy(hostExtraTime = hostExtraTime + numberOfPlayers * hostExtraTimePerPlayer.value)

  def valid =
    if config.limitSeconds + hostExtraTime == LimitSeconds(0)
    then config.incrementSeconds >= 10
    else config.limitSeconds + hostExtraTime > 0
