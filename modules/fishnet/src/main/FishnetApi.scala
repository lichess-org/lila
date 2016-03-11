package lila.fishnet

import lila.db.Implicits._
import reactivemongo.bson._

final class FishnetApi(
    moveColl: Coll,
    analysisColl: Coll,
    clientColl: Coll,
    instanceColl: Coll) {

  import BSONHandlers._

  def getClient(id: String) = clientColl.find(selectId(id)).one[Client]
  def getInstance(id: String) = clientColl.find(selectId(id)).one[Instance]

  private def selectId(id: String) = BSONDocument("_id" -> id)
}
