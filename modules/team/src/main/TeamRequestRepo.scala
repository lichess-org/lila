package lila.team

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final class TeamRequestRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  def exists(teamId: TeamId, userId: UserId): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def find(teamId: TeamId, userId: UserId): Fu[Option[TeamRequest]] =
    coll.one[TeamRequest](selectId(teamId, userId))

  def countDeclinedByTeam(teamId: TeamId): Fu[Int] =
    coll.countSel(teamDeclinedQuery(teamId))

  def findActiveByTeam(teamId: TeamId, nb: Int): Fu[List[TeamRequest]] =
    coll.list[TeamRequest](teamActiveQuery(teamId), nb)

  def findDeclinedByTeam(teamId: TeamId, nb: Int): Fu[List[TeamRequest]] =
    coll.list[TeamRequest](teamDeclinedQuery(teamId), nb)

  def findActiveByTeams(teamIds: List[TeamId]): Fu[List[TeamRequest]] =
    teamIds.nonEmpty.so(coll.list[TeamRequest](teamsActiveQuery(teamIds)))

  def selectId(teamId: TeamId, userId: UserId) = $id(TeamRequest.makeId(teamId, userId))
  def teamQuery(teamId: TeamId)                = $doc("team" -> teamId)
  def teamsQuery(teamIds: List[TeamId])        = $doc("team".$in(teamIds))
  def teamDeclinedQuery(teamId: TeamId, userQuery: Option[UserStr] = None) =
    val baseQuery = $and(teamQuery(teamId), $doc("declined" -> true))
    userQuery.fold(baseQuery): userStr =>
      $and(baseQuery, $doc("user" -> userStr.id))

  def teamActiveQuery(teamId: TeamId)         = $and(teamQuery(teamId), $doc("declined".$ne(true)))
  def teamsActiveQuery(teamIds: List[TeamId]) = $and(teamsQuery(teamIds), $doc("declined".$ne(true)))

  def getByUserId(userId: UserId) =
    coll.list[TeamRequest]($doc("user" -> userId))

  def remove(id: TeamRequest.ID) = coll.delete.one($id(id))

  def cancel(teamId: TeamId, user: User): Fu[Boolean] =
    coll.delete.one(selectId(teamId, user.id)).map(_.n == 1)

  def removeByTeam(teamId: TeamId) = coll.delete.one(teamQuery(teamId))

  def removeByUser(userId: UserId) = coll.delete.one($doc("user" -> userId))

  def countPendingForTeams(teams: Iterable[TeamId]): Fu[Int] =
    teams.nonEmpty.so(coll.secondaryPreferred.countSel($doc("team".$in(teams), "declined".$ne(true))))
