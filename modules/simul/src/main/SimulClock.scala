package lila.simul

import chess.Clock.Config
import chess.{ Black, Centis, Clock, ClockPlayer, Color, White }

// All durations are expressed in seconds

case class SimulClock(
    hostConfig: Clock.Config,
    participantConfig: Clock.Config
) {

  def chessClockOf(hostColor: Color) = {

    val whitePlayer = ClockPlayer.withConfig(if (hostColor == White) hostConfig else participantConfig)
    val blackPlayer = ClockPlayer.withConfig(if (hostColor == Black) hostConfig else participantConfig)
    Clock(
      config = hostConfig,
      color = White,
      players = Color.Map(whitePlayer, blackPlayer),
      timer = None
    )
  }
  //def hostExtraMinutes = hostExtraTime / 60
}
