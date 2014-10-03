package lila.tournament

case class LightTournament(
    id: String,
    name: String,
    status: Int) {

  def running = status == Status.Started.id
}

object LightTournament {

  import reactivemongo.bson._
  private[tournament] implicit val lightTournamentBSONHandler = Macros.handler[LightTournament]
}
