package lila.swiss

import org.joda.time.DateTime

import lila.hub.LightTeam.TeamID
import lila.user.User
import lila.db.dsl._
import lila.common.GreatPlayer

final class SwissApi(
    colls: SwissColls
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  def byId(id: Swiss.Id) = colls.swiss.byId[Swiss](id.value)

  def create(data: SwissForm.SwissData, me: User, teamId: TeamID): Fu[Swiss] = {
    val swiss = Swiss(
      _id = Swiss.makeId,
      name = data.name | GreatPlayer.randomName,
      status = Status.Created,
      clock = data.clock,
      variant = data.realVariant,
      rated = data.rated | true,
      round = SwissRound.Number(0),
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

  def pairingsOf(swiss: Swiss) =
    colls.pairing.ext.find($doc("s" -> swiss.id)).sort($sort asc "r").list[SwissPairing]()

  def featuredInTeam(teamId: TeamID): Fu[List[Swiss]] =
    colls.swiss.ext.find($doc("teamId" -> teamId)).sort($sort desc "startsAt").list[Swiss](5)
}
