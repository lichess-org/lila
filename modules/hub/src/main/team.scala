package lila.hub
package team

import reactivemongo.api.bson.Macros.Annotations.Key
import reactivemongo.api.bson.BSONDocument

case class InsertTeam(team: TeamSearch)
case class RemoveTeam(id: TeamId)

trait TeamRepo:
  def filterHideForum(ids: Iterable[TeamId]): Fu[Set[TeamId]]

trait TeamApi:
  def forumAccessOf(teamId: TeamId): Fu[Access]
  def leaderIds(teamId: TeamId): Fu[Set[UserId]]
  def filterUserIdsInTeam[U: UserIdOf](teamId: TeamId, users: Iterable[U]): Fu[Set[UserId]]
  def cursor: reactivemongo.akkastream.AkkaStreamCursor[TeamSearch]

enum Access(val id: Int):
  case None     extends Access(0)
  case Leaders  extends Access(10)
  case Members  extends Access(20)
  case Everyone extends Access(30)
object Access:
  val allInTeam = List(None, Leaders, Members)
  val all       = Everyone :: allInTeam
  val byId      = all.mapBy(_.id)

case class LightTeam(@Key("_id") id: TeamId, name: LightTeam.TeamName, flair: Option[Flair]):
  def pair = id -> name

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

case class TeamSearch(@Key("_id") id: TeamId, name: String, description: Markdown, nbMembers: Int)
