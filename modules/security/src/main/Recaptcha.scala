package lila.security

import play.api.libs.ws.WS
import play.api.mvc.RequestHeader
import play.api.Play.current
import play.api.libs.json._

import lila.common.HTTPRequest

trait Recaptcha {

  def verify(response: String, req: RequestHeader): Fu[Boolean]
}

object RecaptchaSkip extends Recaptcha {

  def verify(response: String, req: RequestHeader) = fuTrue
}

final class RecaptchaGoogle(
    endpoint: String,
    privateKey: String,
    lichessHostname: String
) extends Recaptcha {

  private case class Response(
      success: Boolean,
      hostname: String
  )

  private implicit val responseReader = Json.reads[Response]

  def verify(response: String, req: RequestHeader): Fu[Boolean] = {
    WS.url(endpoint).post(Map(
      "secret" -> Seq(privateKey),
      "response" -> Seq(response),
      "remoteip" -> Seq(HTTPRequest lastRemoteAddress req value)
    )) flatMap {
      case res if res.status == 200 =>
        res.json.validate[Response] match {
          case JsSuccess(res, _) => fuccess {
            res.success && res.hostname == lichessHostname
          }
          case JsError(err) =>
            fufail(s"$err ${~res.body.lines.toList.headOption}")
        }
      case res => fufail(s"${res.status} ${res.body}")
    } recover {
      case e: Exception =>
        logger.warn(s"recaptcha ${e.getMessage}")
        true
    }
  }
}
