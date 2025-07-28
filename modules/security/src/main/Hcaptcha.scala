package lila.security

import play.api.ConfigLoader
import play.api.data.Forms.*
import play.api.data.{ Form, FormBinding }
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.RequestHeader
import com.softwaremill.tagging.*

import lila.common.HTTPRequest
import lila.common.autoconfig.*
import lila.common.config.given
import lila.core.config.*
import lila.core.net.IpAddress
import lila.core.security.{ HcaptchaForm, HcaptchaPublicConfig }
import lila.memo.SettingStore

trait Hcaptcha extends lila.core.security.Hcaptcha:

  def verify(response: String)(using req: RequestHeader): Fu[Hcaptcha.Result]

  def verify()(using play.api.mvc.Request[?], FormBinding): Fu[Hcaptcha.Result] =
    verify(~Hcaptcha.form.bindFromRequest().value.flatten)

object Hcaptcha:

  enum Result(val ok: Boolean):
    case Valid extends Result(true)
    case Disabled extends Result(true)
    case IpFirst extends Result(true)
    case Mobile extends Result(true)
    case Fail extends Result(false)

  val field = "h-captcha-response" -> optional(nonEmptyText)
  val form = Form(single(field))

  private[security] case class Config(
      endpoint: String,
      @ConfigName("public_key") publicKey: String,
      @ConfigName("private_key") privateKey: Secret,
      enabled: Boolean
  ):
    def public = HcaptchaPublicConfig(publicKey, enabled)
  private[security] given ConfigLoader[Config] = AutoConfig.loader[Config]

final class HcaptchaSkip(config: HcaptchaPublicConfig) extends Hcaptcha:

  def form[A](form: Form[A])(using req: RequestHeader): Fu[HcaptchaForm[A]] = fuccess:
    HcaptchaForm(form, config, skip = true)

  def verify(response: String)(using req: RequestHeader) = fuccess(Hcaptcha.Result.Disabled)

final class HcaptchaReal(
    ipTrust: IpTrust,
    ws: StandaloneWSClient,
    netDomain: NetDomain,
    config: Hcaptcha.Config,
    alwaysCaptcha: SettingStore[Boolean] @@ AlwaysCaptcha
)(using Executor)
    extends Hcaptcha:

  import Hcaptcha.Result

  private case class GoodResponse(success: Boolean, hostname: String)
  private given Reads[GoodResponse] = Json.reads[GoodResponse]

  private case class BadResponse(`error-codes`: List[String]):
    def missingInput = `error-codes` contains "missing-input-response"
    override def toString = `error-codes`.mkString(",")
  private given Reads[BadResponse] = Json.reads[BadResponse]

  private object skipIp:
    private val memo = scalalib.cache.HashCodeExpireSetMemo[IpAddress](24.hours)
    def get(using req: RequestHeader): Fu[Boolean] =
      if alwaysCaptcha.get() then fuFalse
      else
        val ip = HTTPRequest.ipAddress(req)
        (!memo.get(ip)).so:
          ipTrust.isSuspicious(ip).not

    def record(using req: RequestHeader) = memo.put(HTTPRequest.ipAddress(req))

  def form[A](form: Form[A])(using req: RequestHeader): Fu[HcaptchaForm[A]] =
    skipIp.get.map: skip =>
      lila.mon.security.hCaptcha
        .form(HTTPRequest.clientName(req), if skip then "skip" else "show")
        .increment()
      HcaptchaForm(form, config.public, skip)

  def verify(response: String)(using req: RequestHeader): Fu[Result] =
    val client = HTTPRequest.clientName(req)
    given Conversion[Result, Fu[Result]] = fuccess
    ws.url(config.endpoint)
      .post(
        Map(
          "secret" -> config.privateKey.value,
          "response" -> response,
          "remoteip" -> HTTPRequest.ipAddress(req).value,
          "sitekey" -> config.publicKey
        )
      )
      .flatMap:
        case res if res.status == 200 =>
          res.body[JsValue].validate[GoodResponse] match
            case JsSuccess(res, _) =>
              lila.mon.security.hCaptcha.hit(client, "success").increment()
              if res.success && res.hostname == netDomain.value then Result.Valid
              else Result.Fail
            case JsError(err) =>
              res.body[JsValue].validate[BadResponse].asOpt match
                case Some(err) if err.missingInput =>
                  if HTTPRequest.apiVersion(req).isDefined then
                    lila.mon.security.hCaptcha.hit(client, "api").increment()
                    Result.Mobile
                  else
                    skipIp.get.map:
                      if _
                      then
                        lila.mon.security.hCaptcha.hit(client, "skip").increment()
                        skipIp.record
                        Result.IpFirst
                      else
                        logger.info(s"hcaptcha missing ${HTTPRequest.printClient(req)}")
                        lila.mon.security.hCaptcha.hit(client, "missing").increment()
                        Result.Fail
                case Some(err) =>
                  lila.mon.security.hCaptcha.hit(client, err.toString).increment()
                  Result.Fail
                case _ =>
                  lila.mon.security.hCaptcha.hit(client, "error").increment()
                  logger.info(s"hcaptcha $err ${res.body}")
                  Result.Fail
        case res =>
          lila.mon.security.hCaptcha.hit(client, res.status.toString).increment()
          logger.info(s"hcaptcha ${res.status} ${res.body}")
          Result.Fail
