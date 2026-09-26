package lila.app
package mashup

import alleycats.Zero

import lila.core.forum.ForumPostMiniView
import lila.swiss.{ Swiss, SwissApi }
import lila.team.{ Team, TeamMember, TeamRepo }
import lila.tournament.{ Tournament, TournamentApi }
import lila.clas.Clas

case class TeamInfo(
    show: Team.TeamShow,
    forum: Option[List[ForumPostMiniView]],
    tours: TeamInfo.PastAndNext
):
  export show.{ leaders, publicLeaders }

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
    teamRepo: TeamRepo,
    forumRecent: lila.forum.RecentTeamPosts,
    tourApi: TournamentApi,
    swissApi: SwissApi,
    lightUserApi: lila.core.user.LightUserApi
)(using Executor):

  import TeamInfo.*

  def apply(t: Team.TeamShow, withForum: Option[TeamMember] => Boolean): Fu[TeamInfo] = for
    forumPosts <- withForum(t.member).optionFu(forumRecent(t.team.id))
    tours <- t.team.enabled.so(tournamentsOf(t.team, 5, 5))
    _ <- lightUserApi.preloadMany:
      t.publicLeaders.map(_.user) ::: forumPosts.so(_.flatMap(_.post.userId))
  yield TeamInfo(t, forumPosts, tours)

  def tournamentsOf(team: Team, nbPast: Int, nbSoon: Int): Fu[PastAndNext] =
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
    (clas.isActive && clas.hasTeam.orZero).so:
      teamRepo.byClasId(clas.id.into(TeamId)).flatMapz(tournamentsOf(_, 1, 1))
