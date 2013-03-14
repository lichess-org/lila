package lila.app
package team

import user.{ User, UserRepo }
import game.{ GameRepo, DbGame }
import forum.PostLiteView
import http.Context

import scalaz.effects._

case class TeamInfo(
    mine: Boolean,
    createdByMe: Boolean,
    requestedByMe: Boolean,
    requests: List[RequestWithUser],
    bestPlayers: List[User],
    averageElo: Int,
    toints: Int,
    forumNbPosts: Int,
    forumPosts: List[PostLiteView]) {

  def hasRequests = requests.nonEmpty
}

object TeamInfo {

  def apply(
    api: TeamApi,
    memberRepo: MemberRepo,
    requestRepo: RequestRepo,
    userRepo: UserRepo,
    getForumNbPosts: String ⇒ IO[Int],
    getForumPosts: String ⇒ IO[List[PostLiteView]])(team: Team, me: Option[User]): IO[TeamInfo] = for {
    requests ← api.requestsWithUsers(team) doIf {
      team.enabled && ~me.map(m ⇒ team.isCreator(m.id))
    }
    mine = ~me.map(m ⇒ api.belongsTo(team.id, m.id))
    requestedByMe ← ~me.map(m ⇒ requestRepo.exists(team.id, m.id)) doUnless mine
    requests ← api.requestsWithUsers(team) doIf {
      team.enabled && ~me.map(m ⇒ team.isCreator(m.id))
    }
    userIds ← memberRepo userIdsByTeamId team.id
    bestPlayers ← userRepo.byIdsSortByElo(userIds, 5)
    averageElo ← userRepo.idsAverageElo(userIds)
    toints ← userRepo.idsSumToints(userIds)
    forumNbPosts ← getForumNbPosts(team.id)
    forumPosts ← getForumPosts(team.id)
  } yield TeamInfo(
    mine = mine,
    createdByMe = ~me.map(m ⇒ team.isCreator(m.id)),
    requestedByMe = requestedByMe,
    requests = requests,
    bestPlayers = bestPlayers,
    averageElo = averageElo,
    toints = toints,
    forumNbPosts = forumNbPosts,
    forumPosts = forumPosts)
}
