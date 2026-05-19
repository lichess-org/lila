package lila.api

import lila.core.id.ClasId
import lila.swiss.{ Swiss, SwissApi }
import lila.tournament.{ Tournament, TournamentApi }
import lila.team.{ Team, TeamApi }
import lila.clas.Clas

final class ClasApi(
    clasApi: lila.clas.ClasApi,
    teamApi: TeamApi,
    swissApi: SwissApi,
    tourApi: TournamentApi
)(using Executor):

  def teamClas(team: Team): Fu[Option[Clas]] =
    team.isClas.so:
      clasApi.clas.byId(team.id.into(ClasId))

  def onSwissCreate(swiss: Swiss): Funit =
    WithStudents(swiss.teamId): students =>
      swissApi.joinManyNoChecks(swiss.id, students)

  def onArenaCreate(tour: Tournament): Funit =
    tour.singleTeamId.so: teamId =>
      WithStudents(teamId): students =>
        tourApi.joinManyNoChecks(tour.id, students, teamId)

  private def WithStudents(teamId: TeamId)(f: List[UserId] => Funit): Funit =
    teamApi
      .team(teamId)
      .map(_.filter(_.isClas))
      .flatMapz: team =>
        clasApi.student
          .activeUserIdsOf(team.id.into(ClasId))
          .flatMap(f)
