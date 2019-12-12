package lila.hub

package object lightTeam {
  type TeamId = String
  type TeamName = String
  case class LightTeam(_id: TeamId, name: TeamName) {
    def id = _id
    def pair = id -> name
  }
}

trait TellMap {

  def tell(id: String, msg: Any): Unit
}
