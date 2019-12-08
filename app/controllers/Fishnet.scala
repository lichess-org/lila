package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.util.{ Success, Failure }

import lila.app._
import lila.common.HTTPRequest
import lila.fishnet.JsonApi.readers._
import lila.fishnet.JsonApi.writers._
import lila.fishnet.{ JsonApi, Work }

final class Fishnet(env: Env) extends LilaController(env) {

  private def api = env.fishnet.api
  private val logger = lila.log("fishnet")

  def acquire = ClientAction[JsonApi.Request.Acquire] { _ => client =>
    api acquire client addEffect { jobOpt =>
      val mon = lila.mon.fishnet.http.acquire(client.skill.toString)
      if (jobOpt.isDefined) mon.hit() else mon.miss()
    } map Right.apply
  }

  def analysis(workId: String) = ClientAction[JsonApi.Request.PostAnalysis] { data => client =>
    import lila.fishnet.FishnetApi._
    def acquireNext = api acquire client map Right.apply
    api.postAnalysis(Work.Id(workId), client, data).flatFold({
      case WorkNotFound => acquireNext
      case GameNotFound => acquireNext
      case NotAcquired => acquireNext
      case WeakAnalysis(_) => acquireNext
      // case WeakAnalysis => fuccess(Left(UnprocessableEntity("Not enough nodes per move")))
      case e => fuccess(Left(InternalServerError(e.getMessage)))
    }, {
      case _: PostAnalysisResult.Complete => acquireNext
      case _: PostAnalysisResult.Partial => fuccess(Left(NoContent))
      case PostAnalysisResult.UnusedPartial => fuccess(Left(NoContent))
    })
  }

  def abort(workId: String) = ClientAction[JsonApi.Request.Acquire] { _ => client =>
    api.abort(Work.Id(workId), client) inject Right(none)
  }

  def keyExists(key: String) = Action.async { _ =>
    api keyExists lila.fishnet.Client.Key(key) map {
      case true => Ok
      case false => NotFound
    }
  }

  def status = Action.async {
    api.status map { Ok(_) }
  }

  private def ClientAction[A <: JsonApi.Request](f: A => lila.fishnet.Client => Fu[Either[Result, Option[JsonApi.Work]]])(implicit reads: Reads[A]) =
    Action.async(parse.tolerantJson) { req =>
      req.body.validate[A].fold(
        err => {
          logger.warn(s"Malformed request: $err\n${req.body}")
          BadRequest(jsonError(JsError toJson err)).fuccess
        },
        data => api.authenticateClient(data, HTTPRequest lastRemoteAddress req) flatMap {
          case Failure(msg) => Unauthorized(jsonError(msg.getMessage)).fuccess
          case Success(client) => f(data)(client).map {
            case Right(Some(work)) => Accepted(Json toJson work)
            case Right(None) => NoContent
            case Left(result) => result
          }
        }
      )
    }
}
