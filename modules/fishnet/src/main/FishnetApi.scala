package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._
import scala.concurrent.Future

import Client.Skill
import lila.db.Implicits._
import lila.hub.FutureSequencer
import lila.hub.{ actorApi => hubApi }

final class FishnetApi(
    hub: lila.hub.Env,
    repo: FishnetRepo,
    moveDb: MoveDB,
    analysisColl: Coll,
    sequencer: FutureSequencer,
    monitor: Monitor,
    saveAnalysis: lila.analyse.Analysis => Funit,
    offlineMode: Boolean)(implicit system: akka.actor.ActorSystem) {

  import FishnetApi._
  import BSONHandlers._

  def authenticateClient(req: JsonApi.Request, ip: Client.IpAddress) = {
    if (offlineMode) repo.getOfflineClient
    else repo.getEnabledClient(req.fishnet.apikey)
  } flatMap {
    _ ?? { client =>
      repo.updateClientInstance(client, req instance ip) map some
    }
  }

  def acquire(client: Client): Fu[Option[JsonApi.Work]] = (client.skill match {
    case Skill.Move     => acquireMove(client)
    case Skill.Analysis => acquireAnalysis(client)
    case Skill.All      => acquireMove(client) orElse acquireAnalysis(client)
  }).chronometer
    .mon(_.fishnet.acquire time client.skill.key)
    .logIfSlow(100, logger)(_ => s"acquire ${client.skill}")
    .result
    .withTimeout(1 second, AcquireTimeout)
    .recover {
      case e: FutureSequencer.Timeout =>
        logger.warn(s"[${client.skill}] Fishnet.acquire ${e.getMessage}")
        none
      case AcquireTimeout =>
        logger.warn(s"[${client.skill}] Fishnet.acquire timed out")
        none
    } >>- monitor.acquire(client)

  private def acquireMove(client: Client): Fu[Option[JsonApi.Work]] = fuccess {
    moveDb.oldestNonAcquired.map(_ assignTo client) map { found =>
      moveDb update found
      JsonApi fromWork found
    }
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

  def postMove(workId: Work.Id, client: Client, data: JsonApi.Request.PostMove): Funit = fuccess {
    moveDb.get(workId).filter(_ isAcquiredBy client) match {
      case None =>
        logger.warn(s"Received unknown or unacquired move $workId by ${client.fullId}")
      case Some(work) => data.move.uci match {
        case Some(uci) =>
          moveDb delete work
          monitor.move(work, client)
          hub.actor.roundMap ! hubApi.map.Tell(work.game.id, hubApi.round.FishnetPlay(uci, work.currentFen))
        case _ =>
          moveDb updateOrGiveUp work.invalid
          monitor.failure(work, client)
          logger.warn(s"Received invalid move ${data.move} by ${client.fullId}")
      }
    }
  }.chronometer.mon(_.fishnet.move.post)
    .logIfSlow(100, logger)(_ => "post move").result

  def postAnalysis(workId: Work.Id, client: Client, data: JsonApi.Request.PostAnalysis): Funit = sequencer {
    repo.getAnalysis(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
      case None =>
        logger.warn(s"Received unknown or unacquired analysis $workId by ${client.fullId}")
        fuccess(none)
      case Some(work) => AnalysisBuilder(client, work, data) flatMap { analysis =>
        monitor.analysis(work, client, data)
        repo.deleteAnalysis(work) inject analysis.some
      } recoverWith {
        case e: AnalysisBuilder.GameIsGone =>
          logger.warn(s"Game ${work.game.id} was deleted by ${work.sender} before analysis completes")
          monitor.analysis(work, client, data)
          repo.deleteAnalysis(work) inject none
        case e: Exception =>
          monitor.failure(work, client)
          logger.warn(s"Received invalid analysis $workId by ${client.fullId}: ${e.getMessage}")
          repo.updateOrGiveUpAnalysis(work.invalid) inject none
      }
    }
  }.chronometer.mon(_.fishnet.analysis.post)
    .logIfSlow(200, logger) { res =>
      s"post analysis for ${res.??(_.id)}"
    }.result
    .flatMap { _ ?? saveAnalysis }

  def abort(workId: Work.Id, client: Client): Funit = sequencer {
    repo.getAnalysis(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
      _ ?? { work =>
        monitor.abort(work, client)
        repo.updateAnalysis(work.abort)
      }
    }
  }

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
      repo addClient client inject client
    }

  private[fishnet] def setClientSkill(key: Client.Key, skill: String) =
    Client.Skill.byKey(skill).fold(fufail[Unit](s"Invalid skill $skill")) { sk =>
      repo getClient key flatten s"No client with key $key" flatMap { client =>
        repo updateClient client.copy(skill = sk)
      }
    }
}

object FishnetApi {

  case object AcquireTimeout extends lila.common.LilaException {
    val message = "FishnetApi.acquire timed out"
  }
}
