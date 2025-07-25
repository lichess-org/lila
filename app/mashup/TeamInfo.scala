package lila.app
package mashup
import lila.core.forum.ForumPostMiniView
import lila.core.user.User
import lila.simul.{ Simul, SimulApi }
import lila.swiss.{ Swiss, SwissApi }
import lila.team.{ RequestWithUser, Team, TeamApi, TeamMember, TeamRequest, TeamRequestRepo }
import lila.tournament.{ Tournament, TournamentApi }

case class TeamInfo(
    withLeaders: Team.WithLeaders,
    member: Option[TeamMember],
    myRequest: Option[TeamRequest],
    subscribed: Boolean,
    requests: List[RequestWithUser],
    forum: Option[List[ForumPostMiniView]],
    tours: TeamInfo.PastAndNext,
    simuls: Seq[Simul]
):
  export withLeaders.{ team, leaders, publicLeaders }

  def mine = member.isDefined
  def ledByMe = member.exists(_.perms.nonEmpty)

  def hasRequests = requests.nonEmpty

  def userIds = forum.so(_.flatMap(_.post.userId))

object TeamInfo:
  val pmAllCost = 5
  val pmAllCredits = 7
  val pmAllDays = 7
  opaque type AnyTour = Either[Tournament, Swiss]
  object AnyTour extends TotalWrapper[AnyTour, Either[Tournament, Swiss]]:
    extension (e: AnyTour)
      def isEnterable = e.fold(_.isEnterable, _.isEnterable)
      def startsAt = e.fold(_.startsAt, _.startsAt)
      def isNowOrSoon = e.fold(_.isNowOrSoon, _.isNowOrSoon)
      def nbPlayers = e.fold(_.nbPlayers, _.nbPlayers)
    def apply(tour: Tournament): AnyTour = Left(tour)
    def apply(swiss: Swiss): AnyTour = Right(swiss)

  case class PastAndNext(past: List[AnyTour], next: List[AnyTour]):
    def nonEmpty = past.nonEmpty || next.nonEmpty

final class TeamInfoApi(
    api: TeamApi,
    forumRecent: lila.forum.RecentTeamPosts,
    tourApi: TournamentApi,
    swissApi: SwissApi,
    simulApi: SimulApi,
    requestRepo: TeamRequestRepo,
    mongoRateLimitApi: lila.memo.MongoRateLimitApi
)(using Executor, Scheduler):

  import TeamInfo.*

  object pmAll:
    val dedup = scalalib.cache.OnceEvery.hashCode[(TeamId, String)](10.minutes)
    val limiter = mongoRateLimitApi[TeamId](
      "team.pm.all",
      credits = pmAllCredits * pmAllCost,
      duration = pmAllDays.days
    )
    def status(id: TeamId): Fu[(Int, Instant)] =
      limiter
        .getSpent(id)
        .map: entry =>
          (pmAllCredits - entry.v / pmAllCost, entry.until)

  def apply(
      team: Team.WithLeaders,
      me: Option[User],
      withForum: Option[TeamMember] => Boolean
  ): Fu[TeamInfo] = for
    member <- me.so(api.memberOf(team.id, _))
    requests <- (team.enabled && member.exists(_.hasPerm(_.Request))).so(api.requestsWithUsers(team.team))
    myRequest <- member.isEmpty.so(me.so(m => requestRepo.find(team.id, m.id)))
    subscribed <- member.so(api.isSubscribed(team.team, _))
    forumPosts <- withForum(member).soFu(forumRecent(team.id))
    tours <- tournaments(team.team, 5, 5)
    simuls <- simulApi.byTeamLeaders(team.id, team.leaders.toSeq)
  yield TeamInfo(
    withLeaders = team,
    member = member,
    myRequest = myRequest,
    subscribed = subscribed,
    requests = requests,
    forum = forumPosts,
    tours = tours,
    simuls = simuls
  )

  def tournaments(team: Team, nbPast: Int, nbSoon: Int): Fu[PastAndNext] =
    tourApi
      .visibleByTeam(team.id, nbPast, nbSoon)
      .zip(swissApi.visibleByTeam(team.id, nbPast, nbSoon))
      .map: (tours, swisses) =>
        PastAndNext(
          past = {
            tours.past.map(AnyTour(_)) ::: swisses.past.map(AnyTour(_))
          }.sortBy(-_.startsAt.toSeconds),
          next = {
            tours.next.map(AnyTour(_)) ::: swisses.next.map(AnyTour(_))
          }.sortBy(_.startsAt.toSeconds)
        )
