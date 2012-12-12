package lila
package team

import user.{ User, UserRepo }
import game.{ GameRepo, DbGame }
import forum.Categ
import http.Context

import scalaz.effects._

case class TeamInfo(
    mine: Boolean,
    createdByMe: Boolean,
    requestedByMe: Boolean,
    requests: List[RequestWithUser],
    bestPlayers: List[User],
    averageElo: Int,
    forumNbPosts: Int) {
  // rank: Option[(Int, Int)],
  // nbPlaying: Int,
  // nbWithMe: Option[Int],
  // nbBookmark: Int,
  // eloWithMe: Option[List[(String, Int)]],
  // eloChart: Option[EloChart],
  // winChart: Option[WinChart],
  // spy: Option[TeamSpy]) {

  def hasRequests = requests.nonEmpty
}

object TeamInfo {

  def apply(
    api: TeamApi,
    memberRepo: MemberRepo,
    requestRepo: RequestRepo,
    userRepo: UserRepo,
    getForumNbPosts: String ⇒ IO[Int])(team: Team, me: Option[User]): IO[TeamInfo] = for {
    mine ← ~me.map(api.belongsTo(team, _))
    requestedByMe ← ~me.map(m ⇒ requestRepo.exists(team.id, m.id)) doUnless mine
    requests ← api.requestsWithUsers(team) doIf {
      team.enabled && ~me.map(m ⇒ team.isCreator(m.id))
    }
    userIds ← memberRepo userIdsByTeamId team.id
    bestPlayers ← userRepo.byIdsSortByElo(userIds, 5)
    averageElo ← userRepo.idsAverageElo(userIds)
    forumNbPosts ← getForumNbPosts(team.id)
  } yield TeamInfo(
    mine = mine,
    createdByMe = ~me.map(m ⇒ team.isCreator(m.id)),
    requestedByMe = requestedByMe,
    requests = requests,
    bestPlayers = bestPlayers,
    averageElo = averageElo,
    forumNbPosts = forumNbPosts)
}
