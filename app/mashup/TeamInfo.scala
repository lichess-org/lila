package lila.app
package mashup

import lila.forum.MiniForumPost
import lila.team.{ Request, RequestRepo, RequestWithUser, Team, TeamApi }
import lila.tournament.{ Tournament, TournamentApi }
import lila.user.User
import lila.swiss.{ Swiss, SwissApi }
import lila.simul.{ Simul, SimulApi }

case class TeamInfo(
    mine: Boolean,
    ledByMe: Boolean,
    myRequest: Option[Request],
    subscribed: Boolean,
    requests: List[RequestWithUser],
    forum: Option[List[MiniForumPost]],
    tours: TeamInfo.PastAndNext,
    simuls: Seq[Simul]
) {

  def hasRequests = requests.nonEmpty

  def userIds = forum.??(_.flatMap(_.userId))
}

object TeamInfo {
  case class AnyTour(any: Either[Tournament, Swiss]) extends AnyVal {
    def isEnterable = any.fold(_.isEnterable, _.isEnterable)
    def startsAt    = any.fold(_.startsAt, _.startsAt)
    def isNowOrSoon = any.fold(_.isNowOrSoon, _.isNowOrSoon)
    def nbPlayers   = any.fold(_.nbPlayers, _.nbPlayers)
  }
  def anyTour(tour: Tournament) = AnyTour(Left(tour))
  def anyTour(swiss: Swiss)     = AnyTour(Right(swiss))

  case class PastAndNext(past: List[AnyTour], next: List[AnyTour]) {
    def nonEmpty = past.nonEmpty || next.nonEmpty
  }
}

final class TeamInfoApi(
    api: TeamApi,
    forumRecent: lila.forum.ForumRecent,
    tourApi: TournamentApi,
    swissApi: SwissApi,
    simulApi: SimulApi,
    requestRepo: RequestRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import TeamInfo._

  def apply(team: Team, me: Option[User], withForum: Boolean => Boolean): Fu[TeamInfo] =
    for {
      requests   <- (team.enabled && me.exists(m => team.leaders(m.id))) ?? api.requestsWithUsers(team)
      mine       <- me.??(m => api.belongsTo(team.id, m.id))
      myRequest  <- !mine ?? me.??(m => requestRepo.find(team.id, m.id))
      subscribed <- me.ifTrue(mine) ?? { api.isSubscribed(team, _) }
      forumPosts <- withForum(mine) ?? forumRecent.team(team.id).dmap(some)
      tours      <- tournaments(team, 5, 5)
      simuls     <- simulApi.byTeamLeaders(team.id, team.leaders.toSeq)
    } yield TeamInfo(
      mine = mine,
      ledByMe = me.exists(m => team.leaders(m.id)),
      myRequest = myRequest,
      subscribed = subscribed,
      requests = requests,
      forum = forumPosts,
      tours = tours,
      simuls = simuls
    )

  def tournaments(team: Team, nbPast: Int, nbSoon: Int): Fu[PastAndNext] =
    tourApi.visibleByTeam(team.id, nbPast, nbSoon) zip swissApi.visibleByTeam(team.id, nbPast, nbSoon) map {
      case (tours, swisses) =>
        PastAndNext(
          past = {
            tours.past.map(anyTour) ::: swisses.past.map(anyTour)
          }.sortBy(-_.startsAt.getSeconds),
          next = {
            tours.next.map(anyTour) ::: swisses.next.map(anyTour)
          }.sortBy(_.startsAt.getSeconds)
        )
    }
}
