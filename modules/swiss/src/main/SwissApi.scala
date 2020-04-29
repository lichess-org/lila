package lila.swiss

import org.joda.time.DateTime

import lila.hub.LightTeam.TeamID
import lila.user.User

final class SwissApi(
    colls: SwissColls
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def create(data: SwissForm.SwissData, me: User, teamId: TeamID): Fu[Swiss] = {
    val swiss = Swiss(
      _id = Swiss.makeId,
      name = data.name,
      status = Status.Created,
      clock = data.clock,
      variant = data.realVariant,
      rated = data.rated | true,
      nbRounds = data.nbRounds,
      nbPlayers = 0,
      createdAt = DateTime.now,
      createdBy = me.id,
      teamId = teamId,
      startsAt = data.startsAt,
      winnerId = none,
      description = data.description,
      hasChat = data.hasChat | true
    )
    colls.swiss.insert.one(swiss) inject swiss
  }
}
