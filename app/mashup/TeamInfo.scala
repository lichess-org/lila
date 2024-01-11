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
    subscribed: Boolean,
    requests: List[RequestWithUser],
    forumPosts: List[MiniForumPost],
    tours: TeamInfo.PastAndNext
) {

  def hasRequests = requests.nonEmpty

  def userIds = forumPosts.flatMap(_.userId)
}

object TeamInfo {
  case class PastAndNext(past: List[Tournament], next: List[Tournament]) {
    def nonEmpty = past.nonEmpty || next.nonEmpty
  }
}

final class TeamInfoApi(
    api: TeamApi,
    forumRecent: lila.forum.Recent,
    tourApi: TournamentApi,
    requestRepo: RequestRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import TeamInfo._

  def apply(team: Team, me: Option[User]): Fu[TeamInfo] =
    for {
      requests      <- (team.enabled && me.exists(m => team.leaders(m.id))) ?? api.requestsWithUsers(team)
      mine          <- me.??(m => api.belongsTo(team.id, m.id))
      requestedByMe <- !mine ?? me.??(m => requestRepo.exists(team.id, m.id))
      subscribed    <- me.ifTrue(mine) ?? { api.isSubscribed(team, _) }
      forumPosts    <- forumRecent.team(team.id)
      tours         <- tournaments(team, 5, 5)
    } yield TeamInfo(
      mine = mine,
      ledByMe = me.exists(m => team.leaders(m.id)),
      requestedByMe = requestedByMe,
      subscribed = subscribed,
      requests = requests,
      forumPosts = forumPosts,
      tours = tours
    )

  def tournaments(team: Team, nbPast: Int, nbSoon: Int): Fu[PastAndNext] =
    tourApi.visibleByTeam(team.id, nbPast, nbSoon) map { case tours =>
      PastAndNext(
        past = {
          tours.past
        }.sortBy(-_.startsAt.getSeconds),
        next = {
          tours.next
        }.sortBy(_.startsAt.getSeconds)
      )
    }
}
