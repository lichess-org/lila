package controllers

import monocle.syntax.all.*
import play.api.libs.json.*
import play.api.mvc.*

import scala.util.{ Failure, Success }

import lila.app.*
import lila.fishnet.JsonApi.readers.given
import lila.fishnet.JsonApi.writers.given
import lila.fishnet.{ JsonApi, Work }

final class Fishnet(env: Env) extends LilaController(env):

  private def api = env.fishnet.api
  private val logger = lila.log("fishnet")

  def acquire(slow: Boolean = false) =
    ClientAction[JsonApi.Request.Acquire] { _ => client =>
      api
        .acquire(client, slow)
        .addEffect: jobOpt =>
          lila.mon.fishnet.http.request(jobOpt.isDefined).increment()
        .map(Right.apply)
    }

  def analysis(workId: String, slow: Boolean = false, stop: Boolean = false) =
    ClientAction[JsonApi.Request.PostAnalysis] { data => client =>
      import lila.fishnet.FishnetApi.*
      def onComplete =
        if stop then fuccess(Left(NoContent))
        else api.acquire(client, slow).map(Right.apply)
      api
        .postAnalysis(Work.Id(workId), client, data)
        .flatMap:
          case PostAnalysisResult.Complete(analysis) =>
            env.round.proxyRepo.updateIfPresent(GameId(analysis.id.value)): g =>
              g.focus(_.metadata.analysed).replace(true)
            onComplete
          case _: PostAnalysisResult.Partial => fuccess(Left(NoContent))
          case PostAnalysisResult.UnusedPartial => fuccess(Left(NoContent))
        .recoverWith:
          case WorkNotFound => onComplete
          case GameNotFound => onComplete
          case NotAcquired => onComplete
          case e => fuccess(Left(InternalServerError(e.getMessage)))
    }

  def abort(workId: String) =
    ClientAction[JsonApi.Request.Acquire] { _ => client =>
      api.abort(Work.Id(workId), client).inject(Right(none))
    }

  def keyExists(key: String) = Anon:
    api
      .keyExists(lila.fishnet.Client.Key(key))
      .map:
        if _ then Ok
        else NotFound

  val status = Anon:
    api.status.map { JsonStrOk(_) }

  private def ClientAction[A <: JsonApi.Request](
      f: A => lila.fishnet.Client => Fu[Either[Result, Option[JsonApi.Work]]]
  )(using Reads[A]) =
    AnonBodyOf(parse.tolerantJson): body =>
      body
        .validate[A]
        .fold(
          err =>
            logger.warn(s"Malformed request: $err\n${body}")
            BadRequest(jsonError(JsError.toJson(err)))
          ,
          data =>
            api.authenticateClient(data, req.ipAddress).flatMap {
              case Failure(msg) => Unauthorized(jsonError(msg.getMessage))
              case Success(client) =>
                f(data)(client).map {
                  case Right(Some(work)) => Accepted(Json.toJson(work))
                  case Right(None) => NoContent
                  case Left(result) => result
                }
            }
        )
