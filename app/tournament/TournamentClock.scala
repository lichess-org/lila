package lila.app
package tournament

// All durations are expressed in seconds
case class TournamentClock(
  limit: Int,
  increment: Int) {

  def limitInMinutes = limit / 60

  def show = limitInMinutes.toString + " + " + increment.toString

  def chessClock = chess.Clock(limit, increment)
}
