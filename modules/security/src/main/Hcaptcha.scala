package lila.security

import io.methvin.play.autoconfig._
import play.api.data.Form
import play.api.data.FormBinding
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.RequestHeader

import lila.common.config._
import lila.common.HTTPRequest

trait Hcaptcha {

  def verify(response: String, req: RequestHeader): Fu[Hcaptcha.Result]

  def verify()(implicit req: play.api.mvc.Request[_], formBinding: FormBinding): Fu[Hcaptcha.Result] =
    verify(~Hcaptcha.form.bindFromRequest().value.flatten, req)
}

object Hcaptcha {

  sealed abstract class Result(val ok: Boolean)
  object Result {
    case object Valid extends Result(true)
    case object Pass  extends Result(true)
    case object Fail  extends Result(false)
  }

  val field = "h-captcha-response" -> optional(nonEmptyText)
  val form  = Form(single(field))

  private[security] case class Config(
      endpoint: String,
      @ConfigName("public_key") publicKey: String,
      @ConfigName("private_key") privateKey: Secret,
      enabled: Boolean
  ) {
    def public = HcaptchaPublicConfig(publicKey, enabled)
  }
  implicit private[security] val configLoader = AutoConfig.loader[Config]
}

object HcaptchaSkip extends Hcaptcha {

  def verify(response: String, req: RequestHeader) = fuccess(Hcaptcha.Result.Valid)

}

final class HcaptchaReal(
    ws: StandaloneWSClient,
    netDomain: NetDomain,
    config: Hcaptcha.Config
)(implicit ec: scala.concurrent.ExecutionContext)
    extends Hcaptcha {

  import Hcaptcha.Result

  private case class GoodResponse(success: Boolean, hostname: String)
  implicit private val goodReader = Json.reads[GoodResponse]

  private case class BadResponse(
      `error-codes`: List[String]
  ) {
    def missingInput      = `error-codes` contains "missing-input-response"
    override def toString = `error-codes` mkString ","
  }
  implicit private val badReader = Json.reads[BadResponse]

  def verify(response: String, req: RequestHeader): Fu[Result] = {
    val client = HTTPRequest clientName req
    ws.url(config.endpoint)
      .post(
        Map(
          "secret"   -> config.privateKey.value,
          "response" -> response,
          "remoteip" -> HTTPRequest.ipAddress(req).value,
          "sitekey"  -> config.publicKey
        )
      ) map {
      case res if res.status == 200 =>
        res.body[JsValue].validate[GoodResponse] match {
          case JsSuccess(res, _) =>
            lila.mon.security.hCaptcha.hit(client, "success").increment()
            if (res.success && res.hostname == netDomain.value) Result.Valid
            else Result.Fail
          case JsError(err) =>
            res.body[JsValue].validate[BadResponse].asOpt match {
              case Some(err) if err.missingInput =>
                logger.info(s"hcaptcha missing ${HTTPRequest printClient req}")
                lila.mon.security.hCaptcha.hit(client, "missing").increment()
                if (HTTPRequest.apiVersion(req).isDefined) Result.Pass else Result.Fail
              case Some(err) =>
                lila.mon.security.hCaptcha.hit(client, err.toString).increment()
                Result.Fail
              case _ =>
                lila.mon.security.hCaptcha.hit(client, "error").increment()
                logger.info(s"hcaptcha $err ${res.body}")
                Result.Fail
            }
        }
      case res =>
        lila.mon.security.hCaptcha.hit(client, res.status.toString).increment()
        logger.info(s"hcaptcha ${res.status} ${res.body}")
        Result.Fail
    }
  }.thenPp
}
