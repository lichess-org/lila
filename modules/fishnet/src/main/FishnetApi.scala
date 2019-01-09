package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.bson._
import scala.util.{ Try, Success, Failure }

import Client.Skill
import lila.common.IpAddress
import lila.db.dsl._
import lila.hub.FutureSequencer

final class FishnetApi(
    repo: FishnetRepo,
    moveDb: MoveDB,
    analysisBuilder: AnalysisBuilder,
    analysisColl: Coll,
    sequencer: FutureSequencer,
    monitor: Monitor,
    sink: lila.analyse.Analyser,
    socketExists: String => Fu[Boolean],
    clientVersion: ClientVersion,
    offlineMode: Boolean,
    analysisNodes: Int
)(implicit system: akka.actor.ActorSystem) {

  import FishnetApi._
  import JsonApi.Request.{ PartialAnalysis, CompleteAnalysis }
  import BSONHandlers._

  def keyExists(key: Client.Key) = repo.getEnabledClient(key).map(_.isDefined)

  def authenticateClient(req: JsonApi.Request, ip: IpAddress): Fu[Try[Client]] = {
    if (offlineMode) repo.getOfflineClient map some
    else repo.getEnabledClient(req.fishnet.apikey)
  } map {
    case None => Failure(new Exception("Can't authenticate: invalid key or disabled client"))
    case Some(client) => clientVersion accept req.fishnet.version map (_ => client)
  } flatMap {
    case Success(client) => repo.updateClientInstance(client, req instance ip) map Success.apply
    case failure => fuccess(failure)
  }

  def acquire(client: Client): Fu[Option[JsonApi.Work]] = (client.skill match {
    case Skill.Move => acquireMove(client)
    case Skill.Analysis => acquireAnalysis(client)
    case Skill.All => acquireMove(client) orElse acquireAnalysis(client)
  }).chronometer
    .mon(_.fishnet.acquire time client.skill.key)
    .logIfSlow(100, logger)(_ => s"acquire ${client.skill}")
    .result
    .recover {
      case e: lila.hub.Duct.Timeout =>
        lila.mon.fishnet.acquire.timeout(client.skill.key)()
        logger.warn(s"[${client.skill}] Fishnet.acquire ${e.getMessage}")
        none
      case e: Exception =>
        logger.error(s"[${client.skill}] Fishnet.acquire ${e.getMessage}")
        none
    }

  private def acquireMove(client: Client): Fu[Option[JsonApi.Work]] =
    moveDb.acquire(client) map { _ map JsonApi.moveFromWork }

  private def acquireAnalysis(client: Client): Fu[Option[JsonApi.Work]] = sequencer {
    analysisColl.find(
      $doc("acquired" $exists false) ++ {
        !client.offline ?? $doc("lastTryByKey" $ne client.key) // client alternation
      }
    ).sort($doc(
        "sender.system" -> 1, // user requests first, then lichess auto analysis
        "createdAt" -> 1 // oldest requests first
      )).uno[Work.Analysis].flatMap {
        _ ?? { work =>
          repo.updateAnalysis(work assignTo client) inject work.some
        }
      }
  }.map { _ map JsonApi.analysisFromWork(analysisNodes) }

  def postMove(workId: Work.Id, client: Client, data: JsonApi.Request.PostMove): Funit = fuccess {
    val measurement = lila.mon.startMeasurement(_.fishnet.move.post)
    moveDb.postResult(workId, client, data, measurement)
  }

  def postAnalysis(workId: Work.Id, client: Client, data: JsonApi.Request.PostAnalysis): Fu[PostAnalysisResult] =
    repo.getAnalysis(workId).flatMap {
      case None =>
        Monitor.notFound(workId, client)
        fufail(WorkNotFound)
      case Some(work) if work isAcquiredBy client =>
        data.completeOrPartial match {
          case complete: CompleteAnalysis => {
            if (complete.weak && work.game.variant.standard) {
              Monitor.weak(work, client, complete)
              repo.updateOrGiveUpAnalysis(work.weak) >> fufail(WeakAnalysis)
            } else analysisBuilder(client, work, complete.analysis) flatMap { analysis =>
              monitor.analysis(work, client, complete)
              repo.deleteAnalysis(work) inject PostAnalysisResult.Complete(analysis)
            }
          } recoverWith {
            case e: Exception =>
              Monitor.failure(work, client, e)
              repo.updateOrGiveUpAnalysis(work.invalid) >> fufail(e)
          }
          case partial: PartialAnalysis => {
            fuccess(work.game.studyId.isDefined) >>| socketExists(work.game.id)
          } flatMap {
            case true =>
              analysisBuilder.partial(client, work, partial.analysis) map { analysis =>
                PostAnalysisResult.Partial(analysis)
              }
            case false => fuccess(PostAnalysisResult.UnusedPartial)
          }
        }
      case Some(work) =>
        Monitor.notAcquired(work, client)
        fufail(NotAcquired)
    }.chronometer.mon(_.fishnet.analysis.post)
      .logIfSlow(200, logger) {
        case PostAnalysisResult.Complete(res) => s"post analysis for ${res.id}"
        case PostAnalysisResult.Partial(res) => s"partial analysis for ${res.id}"
        case PostAnalysisResult.UnusedPartial => s"unused partial analysis"
      }.result
      .flatMap {
        case r @ PostAnalysisResult.Complete(res) => sink save res inject r
        case r @ PostAnalysisResult.Partial(res) => sink progress res inject r
        case r @ PostAnalysisResult.UnusedPartial => fuccess(r)
      }

  def abort(workId: Work.Id, client: Client): Funit = sequencer {
    repo.getAnalysis(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
      _ ?? { work =>
        Monitor.abort(work, client)
        repo.updateAnalysis(work.abort)
      }
    }
  }

  def gameIdExists(gameId: String) = analysisColl.exists($doc("game.id" -> gameId))

  def status = repo.countAnalysis(acquired = false) map { queued =>
    import play.api.libs.json.Json
    Json.obj(
      "analysis" -> Json.obj(
        "queued" -> queued
      )
    )
  }

  private[fishnet] def createClient(userId: Client.UserId, skill: String): Fu[Client] =
    Client.Skill.byKey(skill).fold(fufail[Client](s"Invalid skill $skill")) { sk =>
      val client = Client(
        _id = Client.makeKey,
        userId = userId,
        skill = sk,
        instance = None,
        enabled = true,
        createdAt = DateTime.now
      )
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

  import lila.base.LilaException

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

  sealed trait PostAnalysisResult
  object PostAnalysisResult {
    case class Complete(analysis: lila.analyse.Analysis) extends PostAnalysisResult
    case class Partial(analysis: lila.analyse.Analysis) extends PostAnalysisResult
    case object UnusedPartial extends PostAnalysisResult
  }
}
