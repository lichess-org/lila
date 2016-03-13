package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._

import Client.Skill
import lila.db.Implicits._
import lila.hub.{ actorApi => hubApi }

final class FishnetApi(
    hub: lila.hub.Env,
    repo: FishnetRepo,
    moveColl: Coll,
    analysisColl: Coll,
    clientColl: Coll,
    sequencer: Sequencer,
    saveAnalysis: lila.analyse.Analysis => Funit,
    offlineMode: Boolean) {

  import BSONHandlers._

  def authenticateClient(req: JsonApi.Request) = {
    if (offlineMode) repo.getOfflineClient
    else repo.getEnabledClient(req.fishnet.apikey)
  } flatMap {
    _ ?? { client =>
      repo.updateClientInstance(client, req.instance) map some
    }
  }

  def acquire(client: Client): Fu[Option[JsonApi.Work]] = client.skill match {
    case Skill.Move     => acquireMove(client)
    case Skill.Analysis => acquireAnalysis(client)
    case Skill.All      => acquireMove(client) orElse acquireAnalysis(client)
  }

  private def acquireMove(client: Client): Fu[Option[JsonApi.Work]] = sequencer.move {
    moveColl.find(BSONDocument(
      "acquired" -> BSONDocument("$exists" -> false)
    )).sort(BSONDocument("createdAt" -> 1)).one[Work.Move].flatMap {
      _ ?? { work =>
        repo.updateMove(work assignTo client) zip repo.updateClient(client acquire work) inject work.some
      }
    }
  } map { _ map JsonApi.fromWork }

  private def acquireAnalysis(client: Client): Fu[Option[JsonApi.Work]] = sequencer.analysis {
    analysisColl.find(BSONDocument(
      "acquired" -> BSONDocument("$exists" -> false)
    )).sort(BSONDocument(
      "sender.system" -> 1, // user requests first, then lichess auto analysis
      "createdAt" -> 1 // oldest requests first
    )).one[Work.Analysis].flatMap {
      _ ?? { work =>
        repo.updateAnalysis(work assignTo client) zip repo.updateClient(client acquire work) inject work.some
      }
    }
  } map { _ map JsonApi.fromWork }

  def postMove(workId: Work.Id, client: Client, data: JsonApi.Request.PostMove): Funit = sequencer.move {
    repo.getMove(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
      case None =>
        log.warn(s"Received unknown or unacquired move $workId by ${client.fullId}")
        funit
      case Some(work) => data.move.uci match {
        case Some(uci) =>
          hub.actor.roundMap ! hubApi.map.Tell(work.game.id, hubApi.round.FishnetPlay(uci, work.currentFen))
          repo.deleteMove(work) zip
            repo.updateClient(client success work) void
        case _ =>
          log.warn(s"Received invalid move ${data.move} by ${client.fullId}")
          repo.updateOrGiveUpMove(work.invalid) zip repo.updateClient(client invalid work) void
      }
    }
  }

  def postAnalysis(workId: Work.Id, client: Client, data: JsonApi.Request.PostAnalysis): Funit = sequencer.analysis {
    repo.getAnalysis(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
      case None =>
        log.warn(s"Received unknown or unacquired analysis $workId by ${client.fullId}")
        fuccess(none)
      case Some(work) => AnalysisBuilder(client, work, data.pp).thenPp flatMap { analysis =>
        repo.deleteAnalysis(work) >>
          repo.updateClient(client success work) inject analysis.some
      } recoverWith {
        case e: Exception =>
          log.warn(s"Received invalid analysis $workId by ${client.fullId}: ${e.getMessage}")
          repo.updateOrGiveUpAnalysis(work.invalid) zip
            repo.updateClient(client invalid work) inject none
      }
    }
  } flatMap { _ ?? saveAnalysis }

  private[fishnet] def createClient(key: Client.Key, userId: Client.UserId, skill: String) =
    Client.Skill.byKey(skill).fold(fufail[Unit](s"Invalid skill $skill")) { sk =>
      clientColl.insert(Client(
        _id = key,
        userId = userId,
        skill = sk,
        instance = None,
        enabled = true,
        stats = Stats.empty,
        createdAt = DateTime.now)).void
    }
  private[fishnet] def setClientSkill(key: Client.Key, skill: String) =
    Client.Skill.byKey(skill).fold(fufail[Unit](s"Invalid skill $skill")) { sk =>
      clientColl.update(repo.selectClient(key), BSONDocument("$set" -> BSONDocument("skill" -> sk.key))).void
    }
}
