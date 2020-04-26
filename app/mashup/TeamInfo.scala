package lila.app
package mashup

import lila.forum.MiniForumPost
import lila.team.{ RequestRepo, RequestWithUser, Team, TeamApi }
import lila.tournament.{ Tournament, TournamentApi }
import lila.user.User

case class TeamInfo(
    mine: Boolean,
    ledByMe: Boolean,
    requestedByMe: Boolean,
    requests: List[RequestWithUser],
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
    tourApi: TournamentApi,
    requestRepo: RequestRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(team: Team, me: Option[User]): Fu[TeamInfo] =
    for {
      requests      <- (team.enabled && me.exists(m => team.leaders(m.id))) ?? api.requestsWithUsers(team)
      mine          <- me.??(m => api.belongsTo(team.id, m.id))
      requestedByMe <- !mine ?? me.??(m => requestRepo.exists(team.id, m.id))
      forumPosts    <- forumRecent.team(team.id)
      tours         <- tourApi.visibleByTeam(team.id)
      _ <- tours.nonEmpty ?? {
        teamCached.preloadSet(tours.flatMap(_.teamBattle.??(_.teams)).toSet)
      }
    } yield TeamInfo(
      mine = mine,
      ledByMe = me.exists(m => team.leaders(m.id)),
      requestedByMe = requestedByMe,
      requests = requests,
      forumPosts = forumPosts,
      tournaments = tours
    )
}
