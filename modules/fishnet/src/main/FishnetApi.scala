package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._

import Client.Skill
import lila.db.Implicits._
import lila.hub.{ actorApi => hubApi }

final class FishnetApi(
    hub: lila.hub.Env,
    repo: FishnetRepo,
    moveDb: MoveDB,
    analysisColl: Coll,
    clientColl: Coll,
    sequencer: lila.hub.FutureSequencer,
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

  def acquire(client: Client): Fu[Option[JsonApi.Work]] = (client.skill match {
    case Skill.Move     => acquireMove(client)
    case Skill.Analysis => acquireAnalysis(client)
    case Skill.All      => acquireMove(client) orElse acquireAnalysis(client)
  }) >>- monitor.acquire(client)

  private def acquireMove(client: Client): Fu[Option[JsonApi.Work]] = fuccess {
    moveDb.transaction { implicit tnx =>
      moveDb.find(_.nonAcquired).toList.sortBy(_.createdAt).headOption
        .map(_ assignTo client) ?? { work =>
          moveDb.update(work)
          work.some
        }
    } map JsonApi.fromWork
  }

  private def acquireAnalysis(client: Client): Fu[Option[JsonApi.Work]] = sequencer {
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
  }.map { _ map JsonApi.fromWork }
    .chronometer.logIfSlow(100, "fishnet")(_ => "acquire analysis").result

  def postMove(workId: Work.Id, client: Client, data: JsonApi.Request.PostMove): Funit = fuccess {
    moveDb.transaction { implicit txn =>
      moveDb.get(workId).filter(_ isAcquiredBy client) match {
        case None =>
          log.warn(s"Received unknown or unacquired move $workId by ${client.fullId}")
        case Some(work) => data.move.uci match {
          case Some(uci) =>
            monitor.move(work, client)
            hub.actor.roundMap ! hubApi.map.Tell(work.game.id, hubApi.round.FishnetPlay(uci, work.currentFen))
            moveDb.delete(work)
          case _ =>
            monitor.failure(work, client)
            log.warn(s"Received invalid move ${data.move} by ${client.fullId}")
            moveDb.updateOrGiveUp(work.invalid)
        }
      }
    }
  }.chronometer.mon(_.fishnet.move.post)
    .logIfSlow(100, "fishnet")(_ => "post move").result

  def postAnalysis(workId: Work.Id, client: Client, data: JsonApi.Request.PostAnalysis): Funit = sequencer {
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
  }.chronometer.mon(_.fishnet.move.post)
    .logIfSlow(100, "fishnet")(_ => "post analysis").result
    .flatMap { _ ?? saveAnalysis }

  def abort(workId: Work.Id, client: Client): Funit = sequencer {
    repo.getAnalysis(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
      _ ?? { work =>
        monitor.abort(work, client)
        repo.updateAnalysis(work.abort)
      }
    }
  }.chronometer.logIfSlow(100, "fishnet")(_ => "abort").result

  def prioritaryAnalysisExists(gameId: String): Fu[Boolean] = analysisColl.count(BSONDocument(
    "game.id" -> gameId,
    "sender.system" -> false
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
