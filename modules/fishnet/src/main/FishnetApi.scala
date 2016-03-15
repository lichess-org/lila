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
    monitor: Monitor,
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
        repo.updateMove(work assignTo client) inject work.some
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
        repo.updateAnalysis(work assignTo client) inject work.some
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
          monitor.move(work, client)
          hub.actor.roundMap ! hubApi.map.Tell(work.game.id, hubApi.round.FishnetPlay(uci, work.currentFen))
          repo.deleteMove(work)
        case _ =>
          monitor.failure(work, client)
          log.warn(s"Received invalid move ${data.move} by ${client.fullId}")
          repo.updateOrGiveUpMove(work.invalid)
      }
    }
  }

  def postAnalysis(workId: Work.Id, client: Client, data: JsonApi.Request.PostAnalysis): Funit = sequencer.analysis {
    repo.getAnalysis(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
      case None =>
        log.warn(s"Received unknown or unacquired analysis $workId by ${client.fullId}")
        fuccess(none)
      case Some(work) => AnalysisBuilder(client, work, data) flatMap { analysis =>
        monitor.analysis(work, client, data)
        repo.deleteAnalysis(work) inject analysis.some
      } recoverWith {
        case e: Exception =>
          monitor.failure(work, client)
          log.warn(s"Received invalid analysis $workId by ${client.fullId}: ${e.getMessage}")
          repo.updateOrGiveUpAnalysis(work.invalid) inject none
      }
    }
  } flatMap { _ ?? saveAnalysis }

  def abort(workId: Work.Id, client: Client): Funit = sequencer.analysis {
    repo.getAnalysis(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
      _ ?? { work =>
        monitor.abort(work, client)
        repo.updateAnalysis(work.abort)
      }
    }
  }

  def analysisExists(gameId: String): Fu[Boolean] = analysisColl.count(BSONDocument(
    "game.id" -> gameId
  ).some).map(0!=)

  private[fishnet] def createClient(userId: Client.UserId, skill: String): Fu[Client] =
    Client.Skill.byKey(skill).fold(fufail[Client](s"Invalid skill $skill")) { sk =>
      val client = Client(
        _id = Client.makeKey,
        userId = userId,
        skill = sk,
        instance = None,
        enabled = true,
        createdAt = DateTime.now)
      clientColl.insert(client) inject client
    }

  private[fishnet] def setClientSkill(key: Client.Key, skill: String) =
    Client.Skill.byKey(skill).fold(fufail[Unit](s"Invalid skill $skill")) { sk =>
      clientColl.update(repo.selectClient(key), BSONDocument("$set" -> BSONDocument("skill" -> sk.key))).void
    }
}
