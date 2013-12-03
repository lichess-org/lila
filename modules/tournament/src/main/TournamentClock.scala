package lila.tournament

// All durations are expressed in seconds
case class TournamentClock(limit: Int, increment: Int) {

  def limitInMinutes = limit / 60

  def show = limitInMinutes.toString + " + " + increment.toString

  def chessClock = chess.Clock(limit, increment)
}

object TournamentClock {

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private[tournament] lazy val tube = JsTube(
    Json.reads[TournamentClock],
    Json.writes[TournamentClock]
  )
}
