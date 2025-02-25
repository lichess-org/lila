package lila.fishnet

import reactivemongo.api.bson.*
import scalalib.actor.AsyncActorSequencer

import scala.util.{ Failure, Success, Try }

import lila.core.lilaism.LilaNoStackTrace
import lila.core.net.IpAddress
import lila.db.dsl.{ *, given }

import Client.Skill

final class FishnetApi(
    repo: FishnetRepo,
    analysisBuilder: AnalysisBuilder,
    analysisColl: Coll,
    monitor: Monitor,
    sink: lila.analyse.Analyser,
    socketExists: GameId => Fu[Boolean],
    clientVersion: Client.ClientVersion,
    config: FishnetApi.Config
)(using Executor, Scheduler):

  import FishnetApi.*
  import JsonApi.Request.{ CompleteAnalysis, PartialAnalysis }
  import BSONHandlers.given

  private val workQueue = AsyncActorSequencer(
    maxSize = Max(256),
    timeout = 5.seconds,
    name = "fishnetApi",
    lila.log.asyncActorMonitor.full
  )

  def keyExists(key: Client.Key) = repo.getEnabledClient(key).map(_.isDefined)

  def authenticateClient(req: JsonApi.Request, ip: IpAddress): Fu[Try[Client]] = {
    if config.offlineMode then repo.getOfflineClient.map(some)
    else repo.getEnabledClient(req.fishnet.apikey)
  }.map {
    case None         => Failure(LilaNoStackTrace("Can't authenticate: invalid key or disabled client"))
    case Some(client) => clientVersion.accept(req.fishnet.version).map(_ => client)
  }.flatMap:
    case Success(client) => repo.updateClientInstance(client, req.instance(ip)).map(Success.apply)
    case invalid         => fuccess(invalid)

  def acquire(client: Client, slow: Boolean): Fu[Option[JsonApi.Work]] =
    client.skill
      .match
        case Skill.Move                 => fufail(s"Can't acquire a move directly on lichess! $client")
        case Skill.Analysis | Skill.All => acquireAnalysis(client, slow)
      .monSuccess(_.fishnet.acquire)
      .recover { case e: Exception =>
        logger.error("Fishnet.acquire", e)
        none
      }

  private def acquireAnalysis(client: Client, slow: Boolean): Fu[Option[JsonApi.Work]] =
    workQueue {
      analysisColl
        .find(
          $doc("acquired".$exists(false)) ++ {
            (!client.offline).so($doc("lastTryByKey".$ne(client.key))) // client alternation
          } ++ {
            slow.so($doc("origin".$in(Work.Origin.slowOk)))
          }
        )
        .sort(
          $doc(
            "sender.system" -> 1, // user requests first, then lichess auto analysis
            "createdAt"     -> 1  // oldest requests first
          )
        )
        .one[Work.Analysis]
        .flatMapz: work =>
          repo.updateAnalysis(work.assignTo(client)).inject(work.some): Fu[Option[Work.Analysis]]
    }.map { _.map(JsonApi.analysisFromWork) }

  def postAnalysis(
      workId: Work.Id,
      client: Client,
      data: JsonApi.Request.PostAnalysis
  ): Fu[PostAnalysisResult] =
    repo
      .getAnalysis(workId)
      .flatMap:
        case None =>
          Monitor.notFound(workId, client)
          fufail(WorkNotFound)
        case Some(work) if work.isAcquiredBy(client) =>
          data.completeOrPartial match
            case complete: CompleteAnalysis =>
              {
                analysisBuilder(client, work, complete.analysis).flatMap { analysis =>
                  monitor.analysis(work, client, complete)
                  repo.deleteAnalysis(work).inject(PostAnalysisResult.Complete(analysis))
                }
              }.recoverWith { case e: Exception =>
                Monitor.failure(work, client, e)
                repo.updateOrGiveUpAnalysis(work, _.invalid) >> fufail(e)
              }
            case partial: PartialAnalysis =>
              {
                fuccess(work.game.studyId.isDefined) >>| socketExists(GameId(work.game.id))
              }.flatMap:
                if _ then
                  analysisBuilder.partial(client, work, partial.analysis).map { analysis =>
                    PostAnalysisResult.Partial(analysis)
                  }
                else fuccess(PostAnalysisResult.UnusedPartial)
          : Fu[PostAnalysisResult]
        case Some(work) =>
          Monitor.notAcquired(work, client)
          fufail(NotAcquired)
      .chronometer
      .logIfSlow(200, logger):
        case PostAnalysisResult.Complete(res) => s"post analysis for ${res.id}"
        case PostAnalysisResult.Partial(res)  => s"partial analysis for ${res.id}"
        case PostAnalysisResult.UnusedPartial => s"unused partial analysis"
      .result
      .flatMap:
        case r @ PostAnalysisResult.Complete(res) => sink.save(res).inject(r)
        case r @ PostAnalysisResult.Partial(res)  => sink.progress(res).inject(r)
        case r @ PostAnalysisResult.UnusedPartial => fuccess(r)

  def abort(workId: Work.Id, client: Client): Funit =
    workQueue:
      repo.getAnalysis(workId).map(_.filter(_.isAcquiredBy(client))).flatMapz { work =>
        Monitor.abort(client)
        repo.updateAnalysis(work.abort)
      }

  def userAnalysisExists(gameId: GameId) =
    analysisColl.exists(
      $doc(
        "game.id"       -> gameId,
        "sender.system" -> false
      )
    )

  def status: Fu[JsonStr] = monitor.statusCache.get {}.map(_.json)

  private[fishnet] def createClient(userId: UserId): Fu[Client] =
    val client = Client(
      _id = Client.makeKey,
      userId = userId,
      skill = Skill.Analysis,
      instance = None,
      enabled = true,
      createdAt = nowInstant
    )
    repo.addClient(client).inject(client)

object FishnetApi:

  import lila.core.lilaism.LilaException

  case class Config(offlineMode: Boolean)

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
