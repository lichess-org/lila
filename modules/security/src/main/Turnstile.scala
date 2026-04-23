package lila.security

import play.api.ConfigLoader
import play.api.data.Forms.*
import play.api.data.{ Form, FormBinding }
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.RequestHeader

import lila.common.HTTPRequest
import lila.common.autoconfig.*
import lila.common.config.given
import lila.core.config.*
import lila.core.security.TurnstilePublicConfig

trait Turnstile:

  protected def verify(response: String)(using RequestHeader): Fu[Turnstile.Result]

  def verify()(using req: play.api.mvc.Request[?])(using FormBinding, Executor): Fu[Boolean] =
    verify(~Turnstile.form.bindFromRequest().value.flatten).map: result =>
      lila.mon.security.turnstile
        .hit(
          client = HTTPRequest.clientName(req),
          action = HTTPRequest.actionName(req),
          result = result.toString
        )
        .increment()
      result.ok

object Turnstile:

  enum Result(val ok: Boolean):
    case Valid extends Result(true)
    case Disabled extends Result(true)
    case Missing extends Result(false)
    case InvalidDomain extends Result(false)
    case Fail extends Result(false)
    case InvalidResponse extends Result(true)
    case CfError extends Result(true)
    case NetError extends Result(true)

  val field = "cf-turnstile-response" -> optional(nonEmptyText)
  val form = Form(single(field))

  private[security] case class Config(
      @ConfigName("site_key") siteKey: String,
      @ConfigName("secret_key") secretKey: Secret,
      enabled: Boolean
  ):
    def public = TurnstilePublicConfig(siteKey, enabled)
  private[security] given ConfigLoader[Config] = AutoConfig.loader[Config]

final class TurnstileSkip extends Turnstile:

  protected def verify(response: String)(using req: RequestHeader) = fuccess(Turnstile.Result.Disabled)

final class TurnstileReal(
    ws: StandaloneWSClient,
    netDomain: NetDomain,
    config: Turnstile.Config
)(using Executor)
    extends Turnstile:

  import Turnstile.Result

  private case class GoodResponse(success: Boolean, hostname: String)
  private given Reads[GoodResponse] = Json.reads[GoodResponse]

  private case class BadResponse(`error-codes`: List[String]):
    def missingInput = `error-codes` contains "missing-input-response"
    override def toString = `error-codes`.mkString(",")
  private given Reads[BadResponse] = Json.reads[BadResponse]

  protected def verify(response: String)(using req: RequestHeader): Fu[Result] =
    given Conversion[Result, Fu[Result]] = fuccess
    def logInfo(msg: String) = logger.branch("turnstile").info(s"$msg ${HTTPRequest.printReqAndClient(req)}")
    def missingResponse: Result =
      logInfo("missing")
      Result.Missing
    if response.isEmpty then missingResponse
    else
      ws.url("https://challenges.cloudflare.com/turnstile/v0/siteverify")
        .post:
          Map(
            "secret" -> config.secretKey.value,
            "response" -> response,
            "remoteip" -> HTTPRequest.ipAddress(req).value,
            "sitekey" -> config.siteKey
          )
        .flatMap:
          case res if res.status == 200 =>
            res.body[JsValue].validate[GoodResponse] match
              case JsSuccess(res, _) =>
                if res.hostname != netDomain.value then Result.InvalidDomain
                else if !res.success then
                  logInfo("fail")
                  Result.Fail
                else Result.Valid
              case JsError(_) =>
                res.body[JsValue].validate[BadResponse].asOpt match
                  case Some(err) if err.missingInput => missingResponse
                  case _ =>
                    logInfo(s"error ${res.body.toString.take(400)}")
                    Result.InvalidResponse
          case res =>
            logInfo(s"cf error ${res.body.toString.take(400)}")
            Result.CfError
        .recover:
          case e =>
            logInfo(s"net error ${e.getMessage}")
            Result.NetError
