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

  private case class GoodResponse(
      success: Boolean,
      hostname: String
  )
  implicit private val goodReader = Json.reads[GoodResponse]

  private case class BadResponse(
      `error-codes`: List[String]
  ) {
    def missingInput      = `error-codes` contains "missing-input-response"
    override def toString = `error-codes` mkString ","
  }
  implicit private val badReader = Json.reads[BadResponse]

  def verify(response: String, req: RequestHeader): Fu[Boolean] = {
    val client = HTTPRequest clientName req
    ws.url(config.endpoint)
      .post(
        Map(
          "secret"   -> config.privateKey.value,
          "response" -> response,
          "remoteip" -> HTTPRequest.ipAddress(req).value
        )
      ) flatMap {
      case res if res.status == 200 =>
        res.body[JsValue].validate[GoodResponse] match {
          case JsSuccess(res, _) =>
            lila.mon.security.recaptcha.hit(client, "success")
            fuccess(res.success && res.hostname == netDomain.value)
          case JsError(err) =>
            res.body[JsValue].validate[BadResponse].asOpt.pp match {
              case Some(err) if err.missingInput =>
                logger.warn(s"recaptcha missing ${HTTPRequest printClient req}")
                lila.mon.security.recaptcha.hit(client, "missing")
                fuccess(HTTPRequest.apiVersion(req).isDefined)
              case Some(err) =>
                lila.mon.security.recaptcha.hit(client, err.toString)
                fuccess(false)
              case _ =>
                lila.mon.security.recaptcha.hit(client, "error")
                logger.warn(s"recaptcha $err ${res.body}")
                fuccess(false)
            }
        }
      case res =>
        lila.mon.security.recaptcha.hit(client, res.status.toString)
        logger.warn(s"recaptcha ${res.status} ${res.body}")
        fuccess(false)
    }
  }
}
