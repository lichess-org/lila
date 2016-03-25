package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import Client.Skill
import lila.db.Implicits._
import lila.hub.FutureSequencer

final class FishnetApi(
    repo: FishnetRepo,
    moveDb: MoveDB,
    analysisColl: Coll,
    sequencer: FutureSequencer,
    monitor: Monitor,
    saveAnalysis: lila.analyse.Analysis => Funit,
    offlineMode: Boolean)(implicit system: akka.actor.ActorSystem) {

  import FishnetApi._
  import BSONHandlers._

  def keyExists(key: Client.Key) = repo.getEnabledClient(key).map(_.isDefined)

  def authenticateClient(req: JsonApi.Request, ip: Client.IpAddress): Fu[Try[Client]] = {
    if (offlineMode) repo.getOfflineClient map some
    else repo.getEnabledClient(req.fishnet.apikey)
  } map {
    case None         => Failure(new Exception("Can't authenticate: invalid key or disabled client"))
    case Some(client) => ClientVersion accept req.fishnet.version map (_ => client)
  } flatMap {
    case Success(client) => repo.updateClientInstance(client, req instance ip) map Success.apply
    case failure         => fuccess(failure)
  }

  def acquire(client: Client): Fu[Option[JsonApi.Work]] = (client.skill match {
    case Skill.Move     => acquireMove(client)
    case Skill.Analysis => acquireAnalysis(client)
    case Skill.All      => acquireMove(client) orElse acquireAnalysis(client)
  }).chronometer
    .mon(_.fishnet.acquire time client.skill.key)
    .logIfSlow(100, logger)(_ => s"acquire ${client.skill}")
    .result
    .recover {
      case e: FutureSequencer.Timeout =>
        lila.mon.fishnet.acquire.timeout(client.skill.key)()
        logger.warn(s"[${client.skill}] Fishnet.acquire ${e.getMessage}")
        none
      case e: Exception =>
        logger.error(s"[${client.skill}] Fishnet.acquire ${e.getMessage}")
        none
    }

  private def acquireMove(client: Client): Fu[Option[JsonApi.Work]] =
    moveDb.acquire(client) map { _ map JsonApi.fromWork }

  private def acquireAnalysis(client: Client): Fu[Option[JsonApi.Work]] = sequencer {
    analysisColl.find(BSONDocument(
      "acquired" -> BSONDocument("$exists" -> false),
      "lastTryByKey" -> BSONDocument("$ne" -> client.key) // client alternation
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
    val measurement = lila.mon.startMeasurement(_.fishnet.move.post)
    moveDb.postResult(workId, client, data, measurement)
  }

  def postAnalysis(workId: Work.Id, client: Client, data: JsonApi.Request.PostAnalysis): Funit = sequencer {
    repo.getAnalysis(workId) flatMap {
      case None =>
        Monitor.notFound(workId, client)
        fufail(WorkNotFound)
      case Some(work) if data.weak && work.game.variant.standard =>
        Monitor.weak(work, client, data)
        repo.updateOrGiveUpAnalysis(work.weak) >> fufail(WeakAnalysis)
      case Some(work) if work isAcquiredBy client =>
        AnalysisBuilder(client, work, data) flatMap { analysis =>
          monitor.analysis(work, client, data)
          repo.deleteAnalysis(work) inject analysis
        } recoverWith {
          case e: AnalysisBuilder.GameIsGone =>
            logger.warn(s"Game ${work.game.id} was deleted by ${work.sender} before analysis completes")
            monitor.analysis(work, client, data)
            repo.deleteAnalysis(work) >> fufail(GameNotFound)
          case e: Exception =>
            Monitor.failure(work, client)
            repo.updateOrGiveUpAnalysis(work.invalid) >> fufail(e)
        }
      case Some(work) =>
        Monitor.notAcquired(work, client)
        fufail(NotAcquired)
    }
  }.chronometer.mon(_.fishnet.analysis.post)
    .logIfSlow(200, logger) { res =>
      s"post analysis for ${res.id}"
    }.result
    .flatMap(saveAnalysis)

  def abort(workId: Work.Id, client: Client): Funit = sequencer {
    repo.getAnalysis(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
      _ ?? { work =>
        Monitor.abort(work, client)
        repo.updateAnalysis(work.abort)
      }
    }
  }

  def prioritaryAnalysisInProgress(gameId: String): Fu[Option[Work.InProgress]] =
    repo.getAnalysisByGameId(gameId) map { _.flatMap(_.inProgress) }

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

  import lila.common.LilaException

  case object WeakAnalysis extends LilaException {
    val message = "Analysis nodes per move is too low"
  }

  case object WorkNotFound extends LilaException {
    val message = "The work has disappeared"
  }

  case object GameNotFound extends LilaException {
    val message = "The game has disappeared"
  }

  case object NotAcquired extends LilaException {
    val message = "The work was distributed to someone else"
  }
}
