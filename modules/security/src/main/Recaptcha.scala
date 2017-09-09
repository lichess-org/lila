package lila.security

import play.api.libs.ws.WS
import play.api.mvc.RequestHeader
import play.api.Play.current

import lila.common.PimpedJson._

trait Recaptcha {

  def verify(response: String, req: RequestHeader): Fu[Boolean]
}

object RecaptchaSkip extends Recaptcha {

  def verify(response: String, req: RequestHeader) = fuccess(true)
}

final class RecaptchaGoogle(
    endpoint: String,
    privateKey: String
) extends Recaptcha {

  def verify(response: String, req: RequestHeader) = {
    WS.url(endpoint).post(Map(
      "secret" -> Seq(privateKey),
      "response" -> Seq(response),
      "remoteip" -> Seq(req.remoteAddress)
    )) flatMap {
      case res if res.status == 200 => fuccess(~res.json.boolean("success"))
      case res => fufail(s"[recaptcha] ${res.status} ${res.body}")
    }
  }
}
