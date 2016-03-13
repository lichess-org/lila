package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.Implicits._

private final class FishnetRepo(
    moveColl: Coll,
    analysisColl: Coll,
    clientColl: Coll) {

  import BSONHandlers._

  def getClient(key: Client.Key) = clientColl.find(selectClient(key)).one[Client]
  def getEnabledClient(key: Client.Key) = getClient(key).map { _.filter(_.enabled) }
  def getOfflineClient = getEnabledClient(Client.offline.key) orElse fuccess(Client.offline.some)
  def updateClient(client: Client): Funit = clientColl.update(selectClient(client.key), client, upsert = true).void
  def updateClientInstance(client: Client, instance: Client.Instance): Fu[Client] =
    client.updateInstance(instance).fold(fuccess(client)) { updated =>
      updateClient(updated) inject updated
    }
  def deleteClient(key: Client.Key) = clientColl.remove(selectClient(key))
  def enableClient(key: Client.Key, v: Boolean): Funit =
    clientColl.update(selectClient(key), BSONDocument("$set" -> BSONDocument("enabled" -> v))).void
  def allClients = clientColl.find(BSONDocument()).cursor[Client]().collect[List]()

  def addMove(move: Work.Move) = moveColl.insert(move).void
  def getMove(id: Work.Id) = moveColl.find(selectWork(id)).one[Work.Move]
  def updateMove(move: Work.Move) = moveColl.update(selectWork(move.id), move).void
  def deleteMove(move: Work.Move) = moveColl.remove(selectWork(move.id)).void
  def giveUpMove(move: Work.Move) = deleteMove(move) >>- log.warn(s"Give up on move $move")
  def updateOrGiveUpMove(move: Work.Move) = if (move.isOutOfTries) giveUpMove(move) else updateMove(move)

  def addAnalysis(ana: Work.Analysis) = analysisColl.insert(ana).void
  def getAnalysis(id: Work.Id) = analysisColl.find(selectWork(id)).one[Work.Analysis]
  def updateAnalysis(ana: Work.Analysis) = analysisColl.update(selectWork(ana.id), ana).void
  def deleteAnalysis(ana: Work.Analysis) = analysisColl.remove(selectWork(ana.id)).void
  def giveUpAnalysis(ana: Work.Analysis) = deleteAnalysis(ana) >>- log.warn(s"Give up on analysis $ana")
  def updateOrGiveUpAnalysis(ana: Work.Analysis) = if (ana.isOutOfTries) giveUpAnalysis(ana) else updateAnalysis(ana)

  def similarMoveExists(work: Work.Move): Fu[Boolean] = moveColl.count(BSONDocument(
    "game.id" -> work.game.id,
    "currentFen" -> work.currentFen
  ).some) map (0 !=)

  def getSimilarAnalysis(work: Work.Analysis): Fu[Option[Work.Analysis]] =
    analysisColl.find(BSONDocument("game.id" -> work.game.id)).one[Work.Analysis]

  def selectWork(id: Work.Id) = BSONDocument("_id" -> id.value)
  def selectClient(key: Client.Key) = BSONDocument("_id" -> key.value)
}
