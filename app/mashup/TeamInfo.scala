package lila.app
package mashup

import concurrent.duration.DurationInt

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
):

  def hasRequests = requests.nonEmpty

  def userIds = forum.??(_.flatMap(_.userId))

object TeamInfo:
  val pmAllCost    = 5
  val pmAllCredits = 7
  val pmAllDays    = 7
  opaque type AnyTour = Either[Tournament, Swiss]
  object AnyTour extends TotalWrapper[AnyTour, Either[Tournament, Swiss]]:
    extension (e: AnyTour)
      def isEnterable = e.fold(_.isEnterable, _.isEnterable)
      def startsAt    = e.fold(_.startsAt, _.startsAt)
      def isNowOrSoon = e.fold(_.isNowOrSoon, _.isNowOrSoon)
      def nbPlayers   = e.fold(_.nbPlayers, _.nbPlayers)
    def apply(tour: Tournament): AnyTour = Left(tour)
    def apply(swiss: Swiss): AnyTour     = Right(swiss)

  case class PastAndNext(past: List[AnyTour], next: List[AnyTour]):
    def nonEmpty = past.nonEmpty || next.nonEmpty

final class TeamInfoApi(
    api: TeamApi,
    forumRecent: lila.forum.RecentTeamPosts,
    tourApi: TournamentApi,
    swissApi: SwissApi,
    simulApi: SimulApi,
    requestRepo: RequestRepo,
    mongoRateLimitApi: lila.memo.MongoRateLimitApi
)(using Executor):

  import TeamInfo.*

  lazy val pmAllLimiter = mongoRateLimitApi[TeamId](
    "team.pm.all",
    credits = pmAllCredits * pmAllCost,
    duration = pmAllDays.days
  )
  def pmAllStatus(id: TeamId): Fu[(Int, Instant)] =
    pmAllLimiter.getSpent(id) map { entry =>
      (pmAllCredits - entry.v / pmAllCost, entry.until)
    }

  def apply(team: Team, me: Option[User], withForum: Boolean => Boolean): Fu[TeamInfo] =
    for {
      requests   <- (team.enabled && me.exists(m => team.leaders(m.id))) ?? api.requestsWithUsers(team)
      mine       <- me.??(m => api.belongsTo(team.id, m.id))
      myRequest  <- !mine ?? me.??(m => requestRepo.find(team.id, m.id))
      subscribed <- me.ifTrue(mine) ?? { api.isSubscribed(team, _) }
      forumPosts <- withForum(mine) ?? forumRecent(team.id).dmap(some)
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
            tours.past.map(AnyTour(_)) ::: swisses.past.map(AnyTour(_))
          }.sortBy(-_.startsAt.toSeconds),
          next = {
            tours.next.map(AnyTour(_)) ::: swisses.next.map(AnyTour(_))
          }.sortBy(_.startsAt.toSeconds)
        )
    }
