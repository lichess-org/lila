package lila.app
package mashup

import lila.forum.MiniForumPost
import lila.team.{ RequestRepo, RequestWithUser, Team, TeamApi }
import lila.tournament.{ Tournament, TournamentRepo }
import lila.user.User

case class TeamInfo(
    mine: Boolean,
    createdByMe: Boolean,
    requestedByMe: Boolean,
    requests: List[RequestWithUser],
    forumNbPosts: Int,
    forumPosts: List[MiniForumPost],
    tournaments: List[Tournament]
) {

  def hasRequests = requests.nonEmpty

  def userIds = forumPosts.flatMap(_.userId)
}

final class TeamInfoApi(
    api: TeamApi,
    categApi: lila.forum.CategApi,
    forumRecent: lila.forum.Recent,
    teamCached: lila.team.Cached,
    tournamentRepo: TournamentRepo,
    requestRepo: RequestRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(team: Team, me: Option[User]): Fu[TeamInfo] =
    for {
      requests      <- (team.enabled && me.??(m => team.isCreator(m.id))) ?? api.requestsWithUsers(team)
      mine          <- me.??(m => api.belongsTo(team.id, m.id))
      requestedByMe <- !mine ?? me.??(m => requestRepo.exists(team.id, m.id))
      forumNbPosts  <- categApi.teamNbPosts(team.id)
      forumPosts    <- forumRecent.team(team.id)
      tours         <- tournamentRepo.byTeam(team.id, 10)
      _ <- tours.nonEmpty ?? {
        teamCached.preloadSet(tours.flatMap(_.teamBattle.??(_.teams)).toSet)
      }
    } yield TeamInfo(
      mine = mine,
      createdByMe = ~me.map(m => team.isCreator(m.id)),
      requestedByMe = requestedByMe,
      requests = requests,
      forumNbPosts = forumNbPosts,
      forumPosts = forumPosts,
      tournaments = tours
    )
}
