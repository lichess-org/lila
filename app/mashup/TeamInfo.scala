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
    simuls: Seq[Simul],
    pmAllsLeft: Option[Int],
    pmAllsRefresh: Option[org.joda.time.DateTime]
):

  def hasRequests = requests.nonEmpty

  def userIds = forum.??(_.flatMap(_.userId))

object TeamInfo:
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
)(using scala.concurrent.ExecutionContext):

  import TeamInfo.*

  val pmAllCost = 5
  lazy val pmAllLimiter = mongoRateLimitApi[TeamId](
    "team.pm.all",
    credits = 7 * pmAllCost,
    duration = 7.days
  )

  def apply(team: Team, me: Option[User], withForum: Boolean => Boolean): Fu[TeamInfo] =
    for {
      requests     <- (team.enabled && me.exists(m => team.leaders(m.id))) ?? api.requestsWithUsers(team)
      mine         <- me.??(m => api.belongsTo(team.id, m.id))
      myRequest    <- !mine ?? me.??(m => requestRepo.find(team.id, m.id))
      subscribed   <- me.ifTrue(mine) ?? { api.isSubscribed(team, _) }
      forumPosts   <- withForum(mine) ?? forumRecent(team.id).dmap(some)
      tours        <- tournaments(team, 5, 5)
      simuls       <- simulApi.byTeamLeaders(team.id, team.leaders.toSeq)
      pmAllLimiter <- me.exists(u => team.leaders(u.id)) ?? pmAllLimiter.getSpent(team.id).dmap(some)
    } yield TeamInfo(
      mine = mine,
      ledByMe = me.exists(m => team.leaders(m.id)),
      myRequest = myRequest,
      subscribed = subscribed,
      requests = requests,
      forum = forumPosts,
      tours = tours,
      simuls = simuls,
      pmAllsLeft = pmAllLimiter.map(l => 7 - l.v / pmAllCost),
      pmAllsRefresh = pmAllLimiter.map(_.e)
    )

  def tournaments(team: Team, nbPast: Int, nbSoon: Int): Fu[PastAndNext] =
    tourApi.visibleByTeam(team.id, nbPast, nbSoon) zip swissApi.visibleByTeam(team.id, nbPast, nbSoon) map {
      case (tours, swisses) =>
        PastAndNext(
          past = {
            tours.past.map(AnyTour(_)) ::: swisses.past.map(AnyTour(_))
          }.sortBy(-_.startsAt.getSeconds),
          next = {
            tours.next.map(AnyTour(_)) ::: swisses.next.map(AnyTour(_))
          }.sortBy(_.startsAt.getSeconds)
        )
    }
