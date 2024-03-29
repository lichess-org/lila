package lila.hub
package team

trait TeamRepo:
  def filterHideForum(ids: Iterable[TeamId]): Fu[Set[TeamId]]

case class LightTeam(_id: TeamId, name: LightTeam.TeamName, flair: Option[Flair]):
  inline def id = _id
  def pair      = id -> name

object LightTeam:
  type TeamName = String

  trait Api:
    def async: Getter
    def sync: GetterSync
    def preload(ids: Set[TeamId]): Funit

  private type GetterType          = TeamId => Fu[Option[LightTeam]]
  opaque type Getter <: GetterType = GetterType
  object Getter extends TotalWrapper[Getter, GetterType]

  private type GetterSyncType              = TeamId => Option[LightTeam]
  opaque type GetterSync <: GetterSyncType = GetterSyncType
  object GetterSync extends TotalWrapper[GetterSync, GetterSyncType]
