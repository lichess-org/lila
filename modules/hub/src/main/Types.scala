package lila.hub

trait TellMap {
  def tell(id: String, msg: Any): Unit
}

object LightTeam {
  type TeamID   = String
  type TeamName = String
}
case class LeaderTeam(_id: LightTeam.TeamID, name: LightTeam.TeamName) {
  def id   = _id
  def pair = id -> name
}
