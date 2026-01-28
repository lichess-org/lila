package lila.api

import lila.swiss.{ Swiss, SwissApi }
import lila.core.id.ClasId

final class ClasApi(clasApi: lila.clas.ClasApi, teamApi: lila.team.TeamApi, swissApi: SwissApi)(using
    Executor
):

  def onSwissCreate(swiss: Swiss) =
    teamApi
      .team(swiss.teamId)
      .map(_.filter(_.isClas))
      .flatMapz: team =>
        clasApi
          .byId(team.id.into(ClasId))
          .flatMapz: clas =>
            ???
