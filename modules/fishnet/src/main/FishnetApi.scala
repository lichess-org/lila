package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.Implicits._

final class FishnetApi(
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
        log.warn(s"Received unknown or unacquired move $moveId by ${client.userId}")
        funit
      case Some(move) => 

  }

  private[fishnet] def addMove(move: Work.Move) = moveColl.insert(move).void

  private[fishnet] def getClient(key: Client.Key) = clientColl.find(selectKey(key)).one[Client]
  private[fishnet] def getEnabledClient(key: Client.Key) = getClient(key).map { _.filter(_.enabled) }
  private[fishnet] def updateClient(client: Client): Funit = clientColl.update(selectKey(client.key), client).void
  private[fishnet] def updateClientInstance(client: Client, instance: Client.Instance): Fu[Client] =
    client.updateInstance(req.instance).fold(fuccess(client)) { updated =>
      updateClient(updated) inject updated
    }
  private[fishnet] def getMove(id: Work.Id) = moveColl.find(selectId(id)).one[Move]
  private[fishnet] def updateMove(move: Work.Move) = moveColl.update(selectId(move.id), move)

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

  private def selectId(id: String) = BSONDocument("_id" -> id)
  private def selectId(id: Work.Id) = BSONDocument("_id" -> id.value)
  private def selectKey(key: Client.Key) = selectId(key.value)
}
