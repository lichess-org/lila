package lila.hub

trait TellMap[Id]:
  def tell(id: Id, msg: Matchable): Unit

object LightTeam:
  type TeamName = String

case class LightTeam(_id: TeamId, name: LightTeam.TeamName):
  inline def id = _id
  def pair      = id -> name
