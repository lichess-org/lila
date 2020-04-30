package lila.swiss

import org.joda.time.DateTime
import reactivemongo.api._

import lila.common.GreatPlayer
import lila.db.dsl._
import lila.hub.LightTeam.TeamID
import lila.user.User

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

  def leaderboard(swiss: Swiss, page: Int): Fu[List[LeaderboardPlayer]] =
    colls.player
      .aggregateList(maxDocs = 10, readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
        import framework._
        Match($doc("s" -> swiss.id)) -> List(
          Sort(Descending("t")),
          Skip((page - 1) * 10),
          Limit(10),
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from" -> colls.pairing.name,
                "let"  -> $doc("n" -> "$n"),
                "pipeline" -> $arr(
                  $doc(
                    "$match" -> $doc(
                      "$expr" -> $doc(
                        "$and" -> $arr(
                          $doc("s" -> swiss.id),
                          $doc("u" -> "$$n")
                        )
                      )
                    )
                  )
                ),
                "as" -> "pairings"
              )
            )
          )
        )
      }
      .map {
        _ map { doc =>
          LeaderboardPlayer(
            playerHandler.read(doc),
            (~doc.getAsOpt[List[SwissPairing]]("pairings")).map { p =>
              p.round -> p
            }.toMap
          )
        }
      }

  def pairingsOf(swiss: Swiss) =
    colls.pairing.ext.find($doc("s" -> swiss.id)).sort($sort asc "r").list[SwissPairing]()

  def featuredInTeam(teamId: TeamID): Fu[List[Swiss]] =
    colls.swiss.ext.find($doc("teamId" -> teamId)).sort($sort desc "startsAt").list[Swiss](5)

  private def insertPairing(pairing: SwissPairing) =
    colls.pairing.insert.one {
      pairingHandler.write(pairing) ++ $doc("d" -> DateTime.now)
    }.void
}
