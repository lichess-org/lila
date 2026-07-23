package controllers

import monocle.syntax.all.*
import cats.mtl.Handle.*
import play.api.libs.json.*
import play.api.mvc.*

import scala.util.{ Failure, Success }

import lila.app.*
import lila.common.HTTPRequest
import lila.fishnet.JsonApi.readers.given
import lila.fishnet.JsonApi.writers.given
import lila.fishnet.{ Client, JsonApi, Work }

final class Fishnet(env: Env) extends LilaController(env):

  private def api = env.fishnet.api

  def acquire(slow: Boolean = false) =
    ClientAction[JsonApi.Request.Acquire] { client => _ =>
      api
        .acquire(client, slow)
        .addEffect: jobOpt =>
          lila.mon.fishnet.http.request(jobOpt.isDefined).increment()
    }

  def analysis(workId: String, slow: Boolean = false, stop: Boolean = false) =
    ClientAction[JsonApi.Request.PostAnalysis] { client => data =>
      import lila.fishnet.FishnetApi.*
      def onComplete =
        if stop then NoContent.raise
        else api.acquire(client, slow)
      allow[Error]:
        api
          .postAnalysis(Work.Id(workId), client, data)
          .flatMap:
            case PostAnalysisResult.Complete(analysis) =>
              env.round.proxyRepo.updateIfPresent(GameId(analysis.id.value)): g =>
                g.focus(_.metadata.analysed).replace(true)
              onComplete
            case _: PostAnalysisResult.Partial => NoContent.raise
            case PostAnalysisResult.UnusedPartial => NoContent.raise
      .rescue:
        case _: Error => onComplete
    }

  def abort(workId: String) =
    ClientAction[JsonApi.Request.Acquire] { client => _ =>
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
      f: lila.fishnet.Client => A => FuRaise[Result, Option[JsonApi.Work]]
  )(using Reads[A]) =
    AnonBodyOf(parse.tolerantJson): body =>
      (HTTPRequest.bearer(req), Client.Version.readFromUA).tupled.so: (bearer, version) =>
        api.authenticateClient(bearer.into(Client.Key), version, req.ipAddress).flatMap {
          case Failure(msg) => Unauthorized(jsonError(msg.getMessage))
          case Success(client) =>
            if !JsonApi.Request.isValid(body) then BadRequest
            else
              body
                .validate[A]
                .fold(
                  err =>
                    lila.fishnet.logger.warn(s"Malformed request: $err\n${body}")
                    BadRequest(jsonError(JsError.toJson(err)))
                  ,
                  data =>
                    allow:
                      f(client)(data).map:
                        _.map(Json.toJson).fold(NoContent)(Accepted(_))
                    .rescue(identity)
                )
        }
