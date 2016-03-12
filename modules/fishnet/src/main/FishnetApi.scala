package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.Implicits._
import lila.hub.Sequencer

final class FishnetApi(
    moveColl: Coll,
    analysisColl: Coll,
    clientColl: Coll,
    sequencer: lila.hub.FutureSequencer) {

  import BSONHandlers._

  def authenticateClient(req: JsonApi.Request) = getEnabledClient(req.key) flatMap {
    _.map(_ setInstance req.instance) ?? { client =>
      updateClient(client) inject client.some
    }
  }

  def acquire(client: Client): Fu[Option[JsonApi.Work]] = sequencer {
    nextMove.flatMap {
      _ ?? { move =>
        updateMove(move assignTo client) zip updateClient(client acquire move) inject move.some
      }
    }
  } map { _ map JsonApi.fromWork }

  private def getClient(key: Client.Key) = clientColl.find(selectKey(key)).one[Client]
  private def getEnabledClient(key: Client.Key) = getClient(key).map { _.filter(_.enabled) }
  private def updateClient(client: Client) = clientColl.update(selectKey(client.key), client)
  private def updateMove(move: Work.Move) = moveColl.update(selectId(move.id), move)

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
  private def selectKey(key: Client.Key) = selectId(key.value)
}
