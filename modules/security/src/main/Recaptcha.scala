package lila.security

import io.methvin.play.autoconfig._
import play.api.libs.json._
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.RequestHeader

import lila.common.config._
import lila.common.HTTPRequest

trait Recaptcha {

  def verify(response: String, req: RequestHeader): Fu[Boolean]
}

private object Recaptcha {

  case class Config(
      endpoint: String,
      @ConfigName("public_key") publicKey: String,
      @ConfigName("private_key") privateKey: Secret,
      enabled: Boolean
  ) {
    def public = RecaptchaPublicConfig(publicKey, enabled)
  }
  implicit val configLoader = AutoConfig.loader[Config]
}

object RecaptchaSkip extends Recaptcha {

  def verify(response: String, req: RequestHeader) = fuTrue

}

final class RecaptchaGoogle(
    ws: StandaloneWSClient,
    netDomain: NetDomain,
    config: Recaptcha.Config
)(implicit ec: scala.concurrent.ExecutionContext)
    extends Recaptcha {

  private case class Response(
      success: Boolean,
      hostname: String
  )

  implicit private val responseReader = Json.reads[Response]

  def verify(response: String, req: RequestHeader): Fu[Boolean] = {
    ws.url(config.endpoint)
      .post(
        Map(
          "secret"   -> config.privateKey.value,
          "response" -> response,
          "remoteip" -> HTTPRequest.ipAddress(req).value
        )
      ) flatMap {
      case res if res.status == 200 =>
        res.body[JsValue].validate[Response] match {
          case JsSuccess(res, _) =>
            fuccess {
              res.success && res.hostname == netDomain.value
            }
          case JsError(err) =>
            fufail(s"$err ${res.body}")
        }
      case res => fufail(s"${res.status} ${res.body}")
    } recover { case e: Exception =>
      logger.info(s"recaptcha ${e.getMessage}")
      true
    }
  }
}
