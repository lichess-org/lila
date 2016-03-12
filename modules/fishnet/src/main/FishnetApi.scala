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

  def authenticateClient(req: JsonApi.Request) = repo.getEnabledClient(req.fishnet.apikey) flatMap {
    _ ?? { client =>
      repo.updateClientInstance(client, req.instance) map some
    }
  }

  def acquire(client: Client): Fu[Option[JsonApi.Work]] = sequencer {
    moveColl.find(BSONDocument(
      "acquired" -> BSONDocument("$exists" -> false)
    )).sort(BSONDocument("createdAt" -> 1)).one[Work.Move].flatMap {
      _ ?? { move =>
        repo.updateMove(move assignTo client) zip repo.updateClient(client acquire move) inject move.some
      }
    }
  } map { _ map JsonApi.fromWork }

  def postMove(moveId: Work.Id, client: Client, data: JsonApi.Request.PostMove): Fu[Option[JsonApi.Work]] = sequencer {
    repo.getMove(moveId).map(_.filter(_ isAcquiredBy client)) flatMap {
      case None =>
        log.warn(s"Received unknown or unacquired move $moveId by ${client.fullId}")
        funit
      case Some(move) => data.move.uci match {
        case Some(uci) =>
          hub.actor.roundMap ! hubApi.map.Tell(move.game.id, hubApi.round.FishnetPlay(uci, move.currentFen))
          repo.deleteMove(move)
        case _ =>
          log.warn(s"Received invalid move ${data.move} by ${client.fullId}")
          repo.updateOrGiveUpMove(move.invalid) >> repo.updateClient(client invalid move)
      }
    }
  } >> acquire(client)

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

  private[fishnet] object repo {

    def addMove(move: Work.Move) = moveColl.insert(move).void

    def getClient(key: Client.Key) = clientColl.find(selectClient(key)).one[Client]
    def getEnabledClient(key: Client.Key) = getClient(key).map { _.filter(_.enabled) }
    def updateClient(client: Client): Funit = clientColl.update(selectClient(client.key), client).void
    def updateClientInstance(client: Client, instance: Client.Instance): Fu[Client] =
      client.updateInstance(instance).fold(fuccess(client)) { updated =>
        updateClient(updated) inject updated
      }
    def getMove(id: Work.Id) = moveColl.find(selectWork(id)).one[Work.Move]
    def updateMove(move: Work.Move) = moveColl.update(selectWork(move.id), move).void
    def deleteMove(move: Work.Move) = moveColl.remove(selectWork(move.id)).void
    def giveUpMove(move: Work.Move) = deleteMove(move) >>- log.warn(s"Give up on move $move")
    def updateOrGiveUpMove(move: Work.Move) = if (move.isOutOfTries) giveUpMove(move) else updateMove(move)

    def similarMoveExists(move: Work.Move): Fu[Boolean] = moveColl.count(BSONDocument(
      "game.id" -> move.game.id,
      "currentFen" -> move.currentFen
    ).some) map (0 !=)

    def selectWork(id: Work.Id) = BSONDocument("_id" -> id.value)
    def selectClient(key: Client.Key) = BSONDocument("_id" -> key.value)
  }
}
