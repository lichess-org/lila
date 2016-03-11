package lila.fishnet

import lila.db.Implicits._
import org.joda.time.DateTime
import reactivemongo.bson._

final class FishnetApi(
    moveColl: Coll,
    analysisColl: Coll,
    clientColl: Coll,
    instanceColl: Coll) {

  import BSONHandlers._

  def getClient(key: Client.Key) = clientColl.find(selectId(key.value)).one[Client]
  def getInstance(id: Instance.Id) = clientColl.find(selectId(id.value)).one[Instance]
  def getInstance(id: Instance.Id, key: Client.Key) = 
    clientColl.find(selectId(id.value) ++ BSONDocument("clientKey" -> key.value)).one[Instance]

  def createClient(key: String, userId: String, skill: String) =
    Client.Skill.byKey(skill).fold(fufail[Unit](s"Invalid skill $skill")) { sk =>
      clientColl.insert(Client(
        _id = key,
        userId = userId,
        skill = sk,
        enabled = true,
        stats = Stats.empty,
        createdAt = DateTime.now)).void
    }

  def getOrCreateInstance(id: Instance.Id, key: Client.Key) =
    getInstance(id) orElse getClient(key

  private def selectId(id: String) = BSONDocument("_id" -> id)
  private def selectKey(key: Client.Key) = BSONDocument("key" -> key.value)
}
