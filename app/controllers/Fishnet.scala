package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.util.{ Failure, Success }

import lila.app._
import lila.common.HTTPRequest
import lila.fishnet.JsonApi.readers._
import lila.fishnet.JsonApi.writers._
import lila.fishnet.{ JsonApi, Work }

final class Fishnet(env: Env) extends LilaController(env) {

  private def api    = env.fishnet.api
  private val logger = lila.log("fishnet")

  def acquire(slow: Boolean = false) =
    ClientAction[JsonApi.Request.Acquire] { _ => client =>
      api.acquire(client, slow) addEffect { jobOpt =>
        lila.mon.fishnet.http.request(jobOpt.isDefined).increment().unit
      } map Right.apply
    }

  def analysis(workId: String, slow: Boolean = false, stop: Boolean = false) =
    ClientAction[JsonApi.Request.PostAnalysis] { data => client =>
      import lila.fishnet.FishnetApi._
      def onComplete =
        if (stop) fuccess(Left(NoContent))
        else api.acquire(client, slow) map Right.apply
      api
        .postAnalysis(Work.Id(workId), client, data)
        .flatFold(
          {
            case WorkNotFound    => onComplete
            case GameNotFound    => onComplete
            case NotAcquired     => onComplete
            case WeakAnalysis(_) => onComplete
            // case WeakAnalysis => fuccess(Left(UnprocessableEntity("Not enough nodes per move")))
            case e => fuccess(Left(InternalServerError(e.getMessage)))
          },
          {
            case PostAnalysisResult.Complete(analysis) =>
              env.round.proxyRepo.updateIfPresent(analysis.id)(_.setAnalysed)
              onComplete
            case _: PostAnalysisResult.Partial    => fuccess(Left(NoContent))
            case PostAnalysisResult.UnusedPartial => fuccess(Left(NoContent))
          }
        )
    }

  def abort(workId: String) =
    ClientAction[JsonApi.Request.Acquire] { _ => client =>
      api.abort(Work.Id(workId), client) inject Right(none)
    }

  def keyExists(key: String) =
    Action.async { _ =>
      api keyExists lila.fishnet.Client.Key(key) map {
        case true  => Ok
        case false => NotFound
      }
    }

  def status =
    Action.async {
      api.status map { Ok(_) }
    }

  private def ClientAction[A <: JsonApi.Request](
      f: A => lila.fishnet.Client => Fu[Either[Result, Option[JsonApi.Work]]]
  )(implicit reads: Reads[A]) =
    Action.async(parse.tolerantJson) { req =>
      req.body
        .validate[A]
        .fold(
          err => {
            logger.warn(s"Malformed request: $err\n${req.body}")
            BadRequest(jsonError(JsError toJson err)).fuccess
          },
          data =>
            api.authenticateClient(data, HTTPRequest ipAddress req) flatMap {
              case Failure(msg) => Unauthorized(jsonError(msg.getMessage)).fuccess
              case Success(client) =>
                f(data)(client).map {
                  case Right(Some(work)) => Accepted(Json toJson work)
                  case Right(None)       => NoContent
                  case Left(result)      => result
                }
            }
        )
    }
}
