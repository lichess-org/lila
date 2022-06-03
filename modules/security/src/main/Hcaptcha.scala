package lila.security

import io.methvin.play.autoconfig._
import play.api.data.Forms._
import play.api.data.{ Form, FormBinding }
import play.api.libs.json._
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.RequestHeader
import scala.concurrent.duration._

import lila.common.config._
import lila.common.HTTPRequest
import lila.common.IpAddress

trait Hcaptcha {

  def form[A](form: Form[A])(implicit req: RequestHeader): Fu[HcaptchaForm[A]]

  def verify(response: String)(implicit req: RequestHeader): Fu[Hcaptcha.Result]

  def verify()(implicit req: play.api.mvc.Request[_], formBinding: FormBinding): Fu[Hcaptcha.Result] =
    verify(~Hcaptcha.form.bindFromRequest().value.flatten)
}

object Hcaptcha {

  sealed abstract class Result(val ok: Boolean)
  object Result {
    case object Valid extends Result(true)
    case object Skip  extends Result(true)
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

final class HcaptchaSkip(config: HcaptchaPublicConfig) extends Hcaptcha {

  def form[A](form: Form[A])(implicit req: RequestHeader): Fu[HcaptchaForm[A]] = fuccess {
    HcaptchaForm(form, config, skip = true)
  }

  def verify(response: String)(implicit req: RequestHeader) = fuccess(Hcaptcha.Result.Valid)

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

  private object skip {
    private val memo = new lila.memo.HashCodeExpireSetMemo[IpAddress](24 hours)

    def get(implicit req: RequestHeader): Boolean       = !memo.get(HTTPRequest ipAddress req)
    def getFu(implicit req: RequestHeader): Fu[Boolean] = fuccess { get }

    def record(implicit req: RequestHeader) = memo.put(HTTPRequest ipAddress req)
  }

  def form[A](form: Form[A])(implicit req: RequestHeader): Fu[HcaptchaForm[A]] =
    skip.getFu map { skip =>
      lila.mon.security.hCaptcha.form(HTTPRequest clientName req, if (skip) "skip" else "show").increment()
      HcaptchaForm(form, config.public, skip)
    }

  def verify(response: String)(implicit req: RequestHeader): Fu[Result] = {
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
                if (HTTPRequest.apiVersion(req).isDefined) {
                  lila.mon.security.hCaptcha.hit(client, "api").increment()
                  Result.Pass
                } else if (skip.get) {
                  lila.mon.security.hCaptcha.hit(client, "skip").increment()
                  skip.record
                  Result.Skip
                } else {
                  logger.info(s"hcaptcha missing ${HTTPRequest printClient req}")
                  lila.mon.security.hCaptcha.hit(client, "missing").increment()
                  Result.Fail
                }
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
  }
}
