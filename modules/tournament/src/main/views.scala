package lila.tournament

case class RoundTournament(
    id: String,
    name: String,
    status: Int,
    system: System,
    clock: TournamentClock) {

  def isRunning = status == Status.Started.id

  def berserkable = system.berserkable && clock.increment == 0
}
