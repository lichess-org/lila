package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.Implicits._
import lila.hub.{ actorApi => hubApi }

final class FishnetApi(
    hub: lila.hub.Env,
    moveColl: Coll,
    analysisColl: Coll,
    clientColl: Coll,
    sequencer: lila.hub.FutureSequencer) {

  import BSONHandlers._

  def authenticateClient(req: JsonApi.Request) = getEnabledClient(req.fishnet.apikey) flatMap {
    _ ?? { client =>
      updateClientInstance(client, req.instance) map some
    }
  }

  def acquire(client: Client): Fu[Option[JsonApi.Work]] = sequencer {
    nextMove.flatMap {
      _ ?? { move =>
        updateMove(move assignTo client) zip updateClient(client acquire move) inject move.some
      }
    }
  } map { _ map JsonApi.fromWork }

  def postMove(moveId: Work.Id, client: Client, data: JsonApi.Request.PostMove): Fu[Option[JsonApi.Work]] = sequencer {
    getMove(moveId).map(_.filter(_ isAcquiredBy client)) flatMap {
      case None =>
        log.warn(s"Received unknown or unacquired move $moveId by ${client.fullId}")
        funit
      case Some(move) => data.move.uci match {
        case Some(uci) =>
          hub.actor.roundMap ! hubApi.map.Tell(move.game.id, hubApi.round.FishnetPlay(uci))
          deleteMove(move)
        case _ =>
          log.warn(s"Received invalid move ${data.move} by ${client.fullId}")
          updateMove(move.invalid) >> updateClient(client invalid move)
      }
    }
  } >> acquire(client)

  private[fishnet] def addMove(move: Work.Move) = moveColl.insert(move).void

  private[fishnet] def getClient(key: Client.Key) = clientColl.find(selectClient(key)).one[Client]
  private[fishnet] def getEnabledClient(key: Client.Key) = getClient(key).map { _.filter(_.enabled) }
  private[fishnet] def updateClient(client: Client): Funit = clientColl.update(selectClient(client.key), client).void
  private[fishnet] def updateClientInstance(client: Client, instance: Client.Instance): Fu[Client] =
    client.updateInstance(instance).fold(fuccess(client)) { updated =>
      updateClient(updated) inject updated
    }
  private[fishnet] def getMove(id: Work.Id) = moveColl.find(selectWork(id)).one[Work.Move]
  private[fishnet] def updateMove(move: Work.Move) = moveColl.update(selectWork(move.id), move).void
  private[fishnet] def deleteMove(move: Work.Move) = moveColl.remove(selectWork(move.id)).void

  private def nextMove: Fu[Option[Work.Move]] = moveColl.find(BSONDocument(
    "acquired" -> BSONDocument("$exists" -> false)
  )).sort(BSONDocument("createdAt" -> -1)).one[Work.Move]

  private[fishnet] def createClient(key: String, userId: String, skill: String) =
    Client.Skill.byKey(skill).fold(fufail[Unit](s"Invalid skill $skill")) { sk =>
      clientColl.insert(Client(
        _id = Client.Key(key),
        userId = Client.UserId(userId),
        skill = sk,
        instance = None,
        enabled = true,
        stats = Stats.empty,
        createdAt = DateTime.now)).void
    }

  private def selectWork(id: Work.Id) = BSONDocument("_id" -> id.value)
  private def selectClient(key: Client.Key) = BSONDocument("_id" -> key.value)
}
