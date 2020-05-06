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
    tours: List[TeamInfo.AnyTour]
) {

  import TeamInfo._

  def hasRequests = requests.nonEmpty

  def userIds = forumPosts.flatMap(_.userId)

  lazy val featuredTours: List[AnyTour] = {
    val (enterable, finished) = tours.partition(_.isEnterable) match {
      case (e, f) => e.sortBy(_.startsAt).take(5) -> f.sortBy(-_.startsAt.getSeconds).take(5)
    }
    enterable ::: finished.take(5 - enterable.size)
  }
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
}

final class TeamInfoApi(
    api: TeamApi,
    forumRecent: lila.forum.Recent,
    teamCached: lila.team.Cached,
    tourApi: TournamentApi,
    swissApi: SwissApi,
    requestRepo: RequestRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import TeamInfo._

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
      tours = tours.map(anyTour) ::: swisses.map(anyTour)
    )

  def tournaments(team: Team, nb: Int): Fu[List[AnyTour]] =
    for {
      tours   <- tourApi.visibleByTeam(team.id, team.leaders, nb)
      swisses <- swissApi.visibleInTeam(team.id, nb)
    } yield {
      tours.map(anyTour) ::: swisses.map(anyTour)
    }.sortBy(-_.startsAt.getSeconds)
}
