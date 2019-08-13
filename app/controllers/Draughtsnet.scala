package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.util.{ Success, Failure }

import lidraughts.app._
import lidraughts.common.HTTPRequest
import lidraughts.draughtsnet.JsonApi.readers._
import lidraughts.draughtsnet.JsonApi.writers._
import lidraughts.draughtsnet.{ JsonApi, Work }

object Draughtsnet extends LidraughtsController {

  private def env = Env.draughtsnet
  private def api = env.api
  private val logger = lidraughts.log("draughtsnet")

  def acquire = ClientAction[JsonApi.Request.Acquire] { req => client =>
    api acquire client addEffect { jobOpt =>
      val mon = lidraughts.mon.draughtsnet.http.acquire(client.skill.toString)
      if (jobOpt.isDefined) mon.hit() else mon.miss()
    } map Right.apply
  }

  def move(workId: String) = ClientAction[JsonApi.Request.PostMove] { data => client =>
    api.postMove(Work.Id(workId), client, data) >>
      api.acquire(client).map(Right.apply)
  }

  def commentary(workId: String) = ClientAction[JsonApi.Request.PostCommentary] { data => client =>
    api.postCommentary(Work.Id(workId), client, data) >>
      api.acquire(client).map(Right.apply)
  }

  def analysis(workId: String) = ClientAction[JsonApi.Request.PostAnalysis] { data => client =>
    import lidraughts.draughtsnet.DraughtsnetApi._
    def acquireNext = api acquire client map Right.apply
    api.postAnalysis(Work.Id(workId), client, data).flatFold({
      case WorkNotFound => acquireNext
      case GameNotFound => acquireNext
      case NotAcquired => acquireNext
      case WeakAnalysis => acquireNext
      // case WeakAnalysis => fuccess(Left(UnprocessableEntity("Not enough nodes per move")))
      case e => fuccess(Left(InternalServerError(e.getMessage)))
    }, {
      case PostAnalysisResult.Complete(analysis) =>
        Env.round.setAnalysedIfPresent(analysis.id)
        acquireNext
      case _: PostAnalysisResult.Partial => fuccess(Left(NoContent))
      case PostAnalysisResult.UnusedPartial => fuccess(Left(NoContent))
    })
  }

  def abort(workId: String) = ClientAction[JsonApi.Request.Acquire] { req => client =>
    api.abort(Work.Id(workId), client) inject Right(none)
  }

  def keyExists(key: String) = Action.async { req =>
    api keyExists lidraughts.draughtsnet.Client.Key(key) map {
      case true => Ok
      case false =>
        val ip = HTTPRequest.lastRemoteAddress(req)
        logger.info(s"Unauthorized key: $key ip: $ip")
        NotFound
    }
  }

  def status = Action.async {
    api.status map { Ok(_) }
  }

  private def ClientAction[A <: JsonApi.Request](f: A => lidraughts.draughtsnet.Client => Fu[Either[Result, Option[JsonApi.Work]]])(implicit reads: Reads[A]) =
    Action.async(BodyParsers.parse.tolerantJson) { req =>
      req.body.validate[A].fold(
        err => {
          logger.warn(s"Malformed request: $err\n${req.body}")
          BadRequest(jsonError(JsError toJson err)).fuccess
        },
        data => api.authenticateClient(data, HTTPRequest lastRemoteAddress req) flatMap {
          case Failure(msg) => {
            val ip = HTTPRequest.lastRemoteAddress(req)
            logger.info(s"Unauthorized key: ${data.draughtsnet.apikey} ip: $ip | ${msg.getMessage}")
            Unauthorized(jsonError(msg.getMessage)).fuccess
          }
          case Success(client) => f(data)(client).map {
            case Right(Some(work)) => Accepted(Json toJson work)
            case Right(None) => NoContent
            case Left(result) => result
          }
        }
      )
    }
}
