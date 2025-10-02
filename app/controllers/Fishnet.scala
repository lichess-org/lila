package controllers

import monocle.syntax.all.*
import cats.mtl.Handle.*
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
    }

  def analysis(workId: String, slow: Boolean = false, stop: Boolean = false) =
    ClientAction[JsonApi.Request.PostAnalysis] { data => client =>
      import lila.fishnet.FishnetApi.*
      def onComplete =
        if stop then NoContent.raise
        else api.acquire(client, slow)
      api
        .postAnalysis(Work.Id(workId), client, data)
        .flatMap:
          case PostAnalysisResult.Complete(analysis) =>
            env.round.proxyRepo.updateIfPresent(GameId(analysis.id.value)): g =>
              g.focus(_.metadata.analysed).replace(true)
            onComplete
          case _: PostAnalysisResult.Partial => NoContent.raise
          case PostAnalysisResult.UnusedPartial => NoContent.raise
        .recoverWith:
          case WorkNotFound => onComplete
          case GameNotFound => onComplete
          case NotAcquired => onComplete
          case e => InternalServerError(e.getMessage).raise
    }

  def abort(workId: String) =
    ClientAction[JsonApi.Request.Acquire] { _ => client =>
      api.abort(Work.Id(workId), client).inject(none)
    }

  def keyExists(key: String) = Anon:
    api
      .keyExists(lila.fishnet.Client.Key(key))
      .map:
        if _ then Ok else NotFound

  val status = Anon:
    api.status.map { JsonStrOk(_) }

  private def ClientAction[A <: JsonApi.Request](
      f: A => lila.fishnet.Client => FuRaise[Result, Option[JsonApi.Work]]
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
                allow:
                  f(data)(client).map:
                    _.map(Json.toJson).fold(NoContent)(Accepted(_))
                .rescue(identity)
            }
        )
