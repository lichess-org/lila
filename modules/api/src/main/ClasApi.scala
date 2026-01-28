package lila.api

import lila.swiss.{ Swiss, SwissApi }
import lila.core.id.ClasId

final class ClasApi(clasApi: lila.clas.ClasApi, teamApi: lila.team.TeamApi, swissApi: SwissApi)(using
    Executor
):

  def onSwissCreate(swiss: Swiss): Funit =
    teamApi
      .team(swiss.teamId)
      .map(_.filter(_.isClas))
      .flatMapz: team =>
        val clasId = team.id.into(ClasId)
        for
          userIds <- clasApi.student.activeUserIdsOf(clasId)
          _ <- swissApi.joinManyNoChecks(swiss.id, userIds)
        yield ()
