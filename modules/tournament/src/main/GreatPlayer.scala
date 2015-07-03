package lila.tournament

final class GreatPlayer(
  val name: String,
  val wiki: String,
  val description: Option[String] = None)

object GreatPlayer {

  val list = List(
    new GreatPlayer("Abbot", "Robert_Abbott_(game_designer)", None),
    new GreatPlayer("Abonyi", "Istv%C3%A1n_Abonyi", Some("Some description of Abonyi"))
  )
}
