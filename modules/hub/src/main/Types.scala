package lila.hub

trait TellMap {
  def tell(id: String, msg: Any): Unit
}

object LightTeam {
  type TeamId = String
  type TeamName = String
}
case class LightTeam(_id: LightTeam.TeamId, name: LightTeam.TeamName) {
  def id = _id
  def pair = id -> name
}
