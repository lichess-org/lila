package lila.swiss

import akka.stream.scaladsl._
import org.joda.time.DateTime
import ornicar.scalalib.Zero
import reactivemongo.api._
import scala.concurrent.duration._

import lila.common.{ Bus, GreatPlayer, LightUser, WorkQueues }
import lila.db.dsl._
import lila.hub.LightTeam.TeamID
import lila.user.User

final class SwissApi(
    colls: SwissColls,
    socket: SwissSocket
)(
    implicit ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer,
    mode: play.api.Mode
) {

  private val workQueue =
    new WorkQueues(buffer = 128, expiration = 1 minute, timeout = 10 seconds, name = "swiss")

  import BsonHandlers._

  def byId(id: Swiss.Id)          = colls.swiss.byId[Swiss](id.value)
  def enterableById(id: Swiss.Id) = colls.swiss.byId[Swiss](id.value).dmap(_.filter(_.isEnterable))

  def create(data: SwissForm.SwissData, me: User, teamId: TeamID): Fu[Swiss] = {
    val swiss = Swiss(
      _id = Swiss.makeId,
      name = data.name | GreatPlayer.randomName,
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
      finishedAt = none,
      winnerId = none,
      description = data.description,
      hasChat = data.hasChat | true
    )
    colls.swiss.insert.one(swiss) inject swiss
  }

  def join(id: Swiss.Id, me: User, isInTeam: TeamID => Boolean): Fu[Boolean] = Sequencing(id)(enterableById) {
    swiss =>
      isInTeam(swiss.teamId) ?? {
        val number = SwissPlayer.Number(swiss.nbPlayers + 1)
        colls.player.insert.one(SwissPlayer.make(swiss.id, number, me, swiss.perfLens)) >>
          updateNbPlayers(swiss.id) >>-
          socket.reload(swiss.id) inject true
      }
  }

  def pairingsOf(swiss: Swiss) = SwissPairing.fields { f =>
    colls.pairing.ext
      .find($doc(f.swissId -> swiss.id))
      .sort($sort asc f.round)
      .list[SwissPairing]()
  }

  def featuredInTeam(teamId: TeamID): Fu[List[Swiss]] =
    colls.swiss.ext.find($doc("teamId" -> teamId)).sort($sort desc "startsAt").list[Swiss](5)

  private def updateNbPlayers(swissId: Swiss.Id): Funit =
    colls.player.countSel($doc(SwissPlayer.Fields.swissId -> swissId)) flatMap {
      colls.swiss.updateField($id(swissId), "nbPlayers", _).void
    }

  private def insertPairing(pairing: SwissPairing) =
    colls.pairing.insert.one {
      pairingHandler.write(pairing) ++ $doc(SwissPairing.Fields.date -> DateTime.now)
    }.void

  private def Sequencing[A: Zero](
      id: Swiss.Id
  )(fetch: Swiss.Id => Fu[Option[Swiss]])(run: Swiss => Fu[A]): Fu[A] =
    workQueue(id.value) {
      fetch(id) flatMap {
        _ ?? run
      }
    }
}
