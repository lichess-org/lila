package lila.hub

trait TellMap[Id <: String]:
  def tell(id: Id, msg: Matchable): Unit

object LightTeam:
  type TeamID   = String
  type TeamName = String
case class LeaderTeam(_id: LightTeam.TeamID, name: LightTeam.TeamName):
  def id   = _id
  def pair = id -> name
