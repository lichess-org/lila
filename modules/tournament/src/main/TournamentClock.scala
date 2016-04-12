package lila.tournament

// All durations are expressed in seconds
case class TournamentClock(limit: Int, increment: Int) {

  def limitInMinutes = chessClock.limitInMinutes

  def show = chessClock.show

  lazy val chessClock = chess.Clock(limit, increment)

  def hasIncrement = increment > 0

  override def toString = show
}
