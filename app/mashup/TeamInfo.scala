package lila.app
package mashup
import lila.core.forum.ForumPostMiniView
import lila.simul.{ Simul, SimulApi }
import lila.swiss.{ Swiss, SwissApi }
import lila.team.{ RequestWithUser, Team, TeamApi, TeamMember, TeamRequest, TeamRepo, TeamRequestRepo }
import lila.tournament.{ Tournament, TournamentApi }
import lila.clas.Clas
import alleycats.Zero

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
  opaque type AnyTour = Tournament | Swiss
  object AnyTour extends TotalWrapper[AnyTour, Tournament | Swiss]:
    extension (e: AnyTour)
      def startsAt = e.fold(_.startsAt, _.startsAt)
      def isRecent = e.startsAt.isAfter(nowInstant.minusDays(1))
      inline def fold[A](ft: Tournament => A, fs: Swiss => A): A = e match
        case t: Tournament => ft(t)
        case s: Swiss => fs(s)

  case class PastAndNext(past: List[AnyTour], next: List[AnyTour]):
    def nonEmpty = past.nonEmpty || next.nonEmpty
  object PastAndNext:
    given Zero[PastAndNext] = Zero(PastAndNext(Nil, Nil))

final class TeamInfoApi(
    api: TeamApi,
    forumRecent: lila.forum.RecentTeamPosts,
    tourApi: TournamentApi,
    swissApi: SwissApi,
    simulApi: SimulApi,
    teamRepo: TeamRepo,
    requestRepo: TeamRequestRepo
)(using Executor):

  import TeamInfo.*

  def apply(
      team: Team.WithLeaders,
      withForum: Option[TeamMember] => Boolean
  )(using me: Option[Me]): Fu[TeamInfo] = for
    member <- me.soUse(api.memberOf(team.id))
    requests <- (team.enabled && member.exists(_.hasPerm(_.Request))).so(api.requestsWithUsers(team.team))
    myRequest <- member.isEmpty.so(me.so(m => requestRepo.find(team.id, m.userId)))
    subscribed <- member.so(api.isSubscribed(team.team, _))
    forumPosts <- withForum(member).optionFu(forumRecent(team.id))
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

  def clasTournaments(clas: Clas): Fu[PastAndNext] =
    clas.hasTeam.orZero.so:
      teamRepo.byClasId(clas.id.into(TeamId)).flatMapz(tournaments(_, 1, 1))
