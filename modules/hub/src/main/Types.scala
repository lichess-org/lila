package lila.hub

trait TellMap[Id]:
  def tell(id: Id, msg: Matchable): Unit

case class LightTeam(_id: TeamId, name: LightTeam.TeamName, flair: Option[Flair]):
  inline def id = _id
  def pair      = id -> name

object LightTeam:
  type TeamName = String

  opaque type GetSync = TeamId => Option[LightTeam]
  object GetSync extends FunctionWrapper[GetSync, TeamId => Option[LightTeam]]
