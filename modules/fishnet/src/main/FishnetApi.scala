package lila.fishnet

import lila.db.Implicits._
import org.joda.time.DateTime
import reactivemongo.bson._

final class FishnetApi(
    moveColl: Coll,
    analysisColl: Coll,
    clientColl: Coll) {

  import BSONHandlers._

  def getClient(key: Client.Key) = clientColl.find(selectId(key.value)).one[Client]

  def createClient(key: String, userId: String, skill: String) =
    Client.Skill.byKey(skill).fold(fufail[Unit](s"Invalid skill $skill")) { sk =>
      clientColl.insert(Client(
        _id = Client.Key(key),
        version = Client.Version("unknown"),
        userId = Client.UserId(userId),
        skill = sk,
        enabled = true,
        stats = Stats.empty,
        createdAt = DateTime.now,
        lastSeenAt = None)).void
    }

  private def selectId(id: String) = BSONDocument("_id" -> id)
}
