package lila.fishnet

import org.joda.time.DateTime
import reactivemongo.api.bson.*
import scala.concurrent.duration.*
import scala.util.{ Failure, Success, Try }

import Client.Skill
import lila.common.IpAddress
import lila.db.dsl.{ *, given }
import lila.game.Game
import lila.common.config.Max

final class FishnetApi(
    repo: FishnetRepo,
    analysisBuilder: AnalysisBuilder,
    analysisColl: Coll,
    monitor: Monitor,
    sink: lila.analyse.Analyser,
    socketExists: GameId => Fu[Boolean],
    clientVersion: Client.ClientVersion,
    config: FishnetApi.Config
)(using scala.concurrent.ExecutionContext, akka.actor.Scheduler):

  import FishnetApi.*
  import JsonApi.Request.{ CompleteAnalysis, PartialAnalysis }
  import BSONHandlers.given

  private val workQueue =
    lila.hub.AsyncActorSequencer(maxSize = Max(256), timeout = 5 seconds, name = "fishnetApi")

  def keyExists(key: Client.Key) = repo.getEnabledClient(key).map(_.isDefined)

  def authenticateClient(req: JsonApi.Request, ip: IpAddress): Fu[Try[Client]] = {
    if (config.offlineMode) repo.getOfflineClient map some
    else repo.getEnabledClient(req.fishnet.apikey)
  } map {
    case None         => Failure(new Exception("Can't authenticate: invalid key or disabled client"))
    case Some(client) => clientVersion accept req.fishnet.version map (_ => client)
  } flatMap {
    case Success(client) => repo.updateClientInstance(client, req instance ip) map Success.apply
    case failure         => fuccess(failure)
  }

  def acquire(client: Client, slow: Boolean): Fu[Option[JsonApi.Work]] =
    (client.skill match {
      case Skill.Move                 => fufail(s"Can't acquire a move directly on lichess! $client")
      case Skill.Analysis | Skill.All => acquireAnalysis(client, slow)
    }).monSuccess(_.fishnet.acquire)
      .recover { case e: Exception =>
        logger.error("Fishnet.acquire", e)
        none
      }

  private def acquireAnalysis(client: Client, slow: Boolean): Fu[Option[JsonApi.Work]] =
    workQueue {
      analysisColl
        .find(
          $doc("acquired" $exists false) ++ {
            !client.offline ?? $doc("lastTryByKey" $ne client.key) // client alternation
          } ++ {
            slow ?? $doc("sender.system" -> true)
          }
        )
        .sort(
          $doc(
            "sender.system" -> 1, // user requests first, then lichess auto analysis
            "createdAt"     -> 1  // oldest requests first
          )
        )
        .one[Work.Analysis]
        .flatMap {
          _ ?? { work =>
            repo.updateAnalysis(work assignTo client) inject work.some: Fu[Option[Work.Analysis]]
          }
        }
    }.map { _ map JsonApi.analysisFromWork(config.analysisNodes) }

  def postAnalysis(
      workId: Work.Id,
      client: Client,
      data: JsonApi.Request.PostAnalysis
  ): Fu[PostAnalysisResult] =
    repo
      .getAnalysis(workId)
      .flatMap {
        case None =>
          Monitor.notFound(workId, client)
          fufail(WorkNotFound)
        case Some(work) if work isAcquiredBy client =>
          data.completeOrPartial match {
            case complete: CompleteAnalysis =>
              {
                analysisBuilder(client, work, complete.analysis) flatMap { analysis =>
                  monitor.analysis(work, client, complete)
                  repo.deleteAnalysis(work) inject PostAnalysisResult.Complete(analysis)
                }
              } recoverWith { case e: Exception =>
                Monitor.failure(work, client, e)
                repo.updateOrGiveUpAnalysis(work, _.invalid) >> fufail(e)
              }
            case partial: PartialAnalysis =>
              {
                fuccess(work.game.studyId.isDefined) >>| socketExists(GameId(work.game.id))
              } flatMap {
                case true =>
                  analysisBuilder.partial(client, work, partial.analysis) map { analysis =>
                    PostAnalysisResult.Partial(analysis)
                  }
                case false => fuccess(PostAnalysisResult.UnusedPartial)
              }
          }: Fu[PostAnalysisResult]
        case Some(work) =>
          Monitor.notAcquired(work, client)
          fufail(NotAcquired)
      }
      .chronometer
      .logIfSlow(200, logger) {
        case PostAnalysisResult.Complete(res) => s"post analysis for ${res.id}"
        case PostAnalysisResult.Partial(res)  => s"partial analysis for ${res.id}"
        case PostAnalysisResult.UnusedPartial => s"unused partial analysis"
      }
      .result
      .flatMap {
        case r @ PostAnalysisResult.Complete(res) => sink save res inject r
        case r @ PostAnalysisResult.Partial(res)  => sink progress res inject r
        case r @ PostAnalysisResult.UnusedPartial => fuccess(r)
      }

  def abort(workId: Work.Id, client: Client): Funit =
    workQueue {
      repo.getAnalysis(workId).map(_.filter(_ isAcquiredBy client)) flatMap {
        _ ?? { work =>
          Monitor.abort(client)
          repo.updateAnalysis(work.abort)
        }
      }
    }

  def userAnalysisExists(gameId: GameId) =
    analysisColl.exists(
      $doc(
        "game.id"       -> gameId,
        "sender.system" -> false
      )
    )

  def status =
    monitor.statusCache.get {} map { c =>
      import play.api.libs.json.Json
      def statusFor(s: Monitor.StatusFor) =
        Json.obj(
          "acquired" -> s.acquired,
          "queued"   -> s.queued,
          "oldest"   -> s.oldest
        )
      Json.obj(
        "analysis" -> Json.obj(
          "user"   -> statusFor(c.user),
          "system" -> statusFor(c.system)
        )
      )
    }

  private[fishnet] def createClient(userId: UserId): Fu[Client] =
    val client = Client(
      _id = Client.makeKey,
      userId = userId,
      skill = Skill.Analysis,
      instance = None,
      enabled = true,
      createdAt = DateTime.now
    )
    repo addClient client inject client

object FishnetApi:

  import lila.base.LilaException

  case class Config(
      offlineMode: Boolean,
      analysisNodes: Int
  )

  case object WorkNotFound extends LilaException:
    val message = "The work has disappeared"

  case object GameNotFound extends LilaException:
    val message = "The game has disappeared"

  case object NotAcquired extends LilaException:
    val message = "The work was distributed to someone else"

  enum PostAnalysisResult:
    case Complete(analysis: lila.analyse.Analysis)
    case Partial(analysis: lila.analyse.Analysis)
    case UnusedPartial
