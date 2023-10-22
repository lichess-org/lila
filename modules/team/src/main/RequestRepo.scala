package lila.team

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }
import lila.user.User

final class RequestRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  def exists(teamId: TeamId, userId: UserId): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def find(teamId: TeamId, userId: UserId): Fu[Option[Request]] =
    coll.one[Request](selectId(teamId, userId))

  def countDeclinedByTeam(teamId: TeamId): Fu[Int] =
    coll.countSel(teamDeclinedQuery(teamId))

  def findActiveByTeam(teamId: TeamId, nb: Int): Fu[List[Request]] =
    coll.list[Request](teamActiveQuery(teamId), nb)

  def findDeclinedByTeam(teamId: TeamId, nb: Int): Fu[List[Request]] =
    coll.list[Request](teamDeclinedQuery(teamId), nb)

  def findActiveByTeams(teamIds: List[TeamId]): Fu[List[Request]] =
    teamIds.nonEmpty so coll.list[Request](teamsActiveQuery(teamIds))

  def selectId(teamId: TeamId, userId: UserId) = $id(Request.makeId(teamId, userId))
  def teamQuery(teamId: TeamId)                = $doc("team" -> teamId)
  def teamsQuery(teamIds: List[TeamId])        = $doc("team" $in teamIds)
  def teamDeclinedQuery(teamId: TeamId)        = $and(teamQuery(teamId), $doc("declined" -> true))
  def teamActiveQuery(teamId: TeamId)          = $and(teamQuery(teamId), $doc("declined" $ne true))
  def teamsActiveQuery(teamIds: List[TeamId])  = $and(teamsQuery(teamIds), $doc("declined" $ne true))

  def getByUserId(userId: UserId) =
    coll.list[Request]($doc("user" -> userId))

  def remove(id: Request.ID) = coll.delete.one($id(id))

  def cancel(teamId: TeamId, user: User): Fu[Boolean] =
    coll.delete.one(selectId(teamId, user.id)).map(_.n == 1)

  def removeByTeam(teamId: TeamId) = coll.delete.one(teamQuery(teamId))

  def removeByUser(userId: UserId) = coll.delete.one($doc("user" -> userId))

  def countForLeader(leader: UserId, memberColl: Coll): Fu[Int] =
    memberColl
      .aggregateOne(_.sec): framework =>
        import framework.*
        Match($doc("user" -> leader, "perms" -> TeamSecurity.Permission.Request)) -> List(
          Group(BSONNull)("teams" -> PushField("team")),
          PipelineOperator(
            $lookup.pipelineFull(
              from = coll.name,
              as = "requests",
              let = $doc("teams" -> "$teams"),
              pipe = List:
                $doc:
                  "$match" -> $expr:
                    $doc:
                      $and(
                        $doc("$in" -> $arr("$team", "$$teams")),
                        $doc("$ne" -> $arr("$declined", true))
                      )
            )
          ),
          Group(BSONNull):
            "nb" -> Sum($doc("$size" -> "$requests"))
        )
      .map(~_.flatMap(_.int("nb")))
