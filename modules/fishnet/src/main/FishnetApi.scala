package lila.fishnet

import lila.db.Implicits._
import org.joda.time.DateTime
import reactivemongo.bson._

final class FishnetApi(
    moveColl: Coll,
    analysisColl: Coll,
    clientColl: Coll) {

  import BSONHandlers._

  def getClient(key: Client.Key) = clientColl.find(selectKey(key)).one[Client]
  def getEnabledClient(key: Client.Key) = getClient(key).map { _.filter(_.enabled) }

  def updateClient(client: Client) = clientColl.update(selectKey(client.key), client)

  def authenticateClient(req: JsonApi.Request) = getEnabledClient(req.key) flatMap {
    _.map(_ setInstance req.instance) ?? { client =>
      updateClient(client) inject client.some
    }
  }

  def acquire(client: Client): Fu[Option[JsonApi.Work]] = ???

  def createClient(key: String, userId: String, skill: String) =
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
