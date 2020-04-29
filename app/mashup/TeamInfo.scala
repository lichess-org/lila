package lila.app
package mashup

import lila.forum.MiniForumPost
import lila.team.{ RequestRepo, RequestWithUser, Team, TeamApi }
import lila.tournament.{ Tournament, TournamentApi }
import lila.user.User
import lila.swiss.{ Swiss, SwissApi }

case class TeamInfo(
    mine: Boolean,
    ledByMe: Boolean,
    requestedByMe: Boolean,
    requests: List[RequestWithUser],
    forumPosts: List[MiniForumPost],
    tournaments: List[Tournament],
    swisses: List[Swiss]
) {

  def hasRequests = requests.nonEmpty

  def userIds = forumPosts.flatMap(_.userId)
}

final class TeamInfoApi(
    api: TeamApi,
    forumRecent: lila.forum.Recent,
    teamCached: lila.team.Cached,
    tourApi: TournamentApi,
    swissApi: SwissApi,
    requestRepo: RequestRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(team: Team, me: Option[User]): Fu[TeamInfo] =
    for {
      requests      <- (team.enabled && me.exists(m => team.leaders(m.id))) ?? api.requestsWithUsers(team)
      mine          <- me.??(m => api.belongsTo(team.id, m.id))
      requestedByMe <- !mine ?? me.??(m => requestRepo.exists(team.id, m.id))
      forumPosts    <- forumRecent.team(team.id)
      tours         <- tourApi.featuredInTeam(team.id)
      swisses       <- swissApi.featuredInTeam(team.id)
    } yield TeamInfo(
      mine = mine,
      ledByMe = me.exists(m => team.leaders(m.id)),
      requestedByMe = requestedByMe,
      requests = requests,
      forumPosts = forumPosts,
      tournaments = tours,
      swisses = swisses
    )
}
