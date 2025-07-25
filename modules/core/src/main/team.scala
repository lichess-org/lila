package lila.core
package team

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.data.Markdown
import lila.core.id.{ Flair, TeamId }
import lila.core.userId.*

trait TeamApi:
  def filterHideForum(ids: Iterable[TeamId]): Fu[Set[TeamId]]
  def forumAccessOf(teamId: TeamId): Fu[Access]
  def leaderIds(teamId: TeamId): Fu[Set[UserId]]
  def belongsTo[U: UserIdOf](teamId: TeamId, u: U): Fu[Boolean]
  def isLeader[U: UserIdOf](team: TeamId, leader: U): Fu[Boolean]
  def filterUserIdsInTeam[U: UserIdOf](teamId: TeamId, users: Iterable[U]): Fu[Set[UserId]]
  def hasCommPerm(team: TeamId, userId: UserId): Fu[Boolean]
  def cursor: reactivemongo.akkastream.AkkaStreamCursor[TeamData]

enum Access(val id: Int):
  case None extends Access(0)
  case Leaders extends Access(10)
  case Members extends Access(20)
  case Everyone extends Access(30)
object Access:
  val allInTeam = List(None, Leaders, Members)
  val all = Everyone :: allInTeam
  val byId = all.mapBy(_.id)

case class LightTeam(@Key("_id") id: TeamId, name: LightTeam.TeamName, flair: Option[Flair]):
  def pair = id -> name

object LightTeam:
  type TeamName = String

  trait Api:
    def async: Getter
    def sync: GetterSync
    def preload(ids: Set[TeamId]): Funit

  private type GetterType = TeamId => Fu[Option[LightTeam]]
  opaque type Getter <: GetterType = GetterType
  object Getter extends TotalWrapper[Getter, GetterType]

  private type GetterSyncType = TeamId => Option[LightTeam]
  opaque type GetterSync <: GetterSyncType = GetterSyncType
  object GetterSync extends TotalWrapper[GetterSync, GetterSyncType]

case class TeamData(
    @Key("_id") id: TeamId,
    name: String,
    description: Markdown,
    nbMembers: Int,
    userId: UserId
)
case class TeamCreate(team: TeamData)
case class TeamUpdate(team: TeamData, byMod: Boolean)(using val me: MyId)
case class JoinTeam(id: TeamId, userId: UserId)
case class IsLeaderOf(leaderId: UserId, memberId: UserId, promise: Promise[Boolean])
case class IsLeaderWithCommPerm(id: TeamId, userId: UserId, promise: Promise[Boolean])
case class KickFromTeam(teamId: TeamId, teamName: String, userId: UserId)(using val me: MyId)
case class LeaveTeam(teamId: TeamId, userId: UserId)
case class TeamIdsJoinedBy(userId: UserId, promise: Promise[List[TeamId]])
