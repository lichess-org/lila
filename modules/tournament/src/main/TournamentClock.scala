package lila.tournament

// All durations are expressed in seconds
case class TournamentClock(limit: Int, increment: Int) {

  def limitInMinutes = limit / 60

  def show = s"${limitInMinutes}+${increment}"

  def chessClock = chess.Clock(limit, increment)
}
