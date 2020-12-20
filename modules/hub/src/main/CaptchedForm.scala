package lila.hub

import akka.pattern.ask
import play.api.data._
import scala.concurrent.duration._

import actorApi.captcha._
import lila.common.Captcha

trait CaptchedForm {

  import makeTimeout.large

  type CaptchedData = {
    def gameId: String
    def move: String
  }

  val captcher: actors.Captcher

  def anyCaptcha: Fu[Captcha] =
    (captcher.actor ? AnyCaptcha).mapTo[Captcha]

  def getCaptcha(id: String): Fu[Captcha] =
    (captcher.actor ? GetCaptcha(id)).mapTo[Captcha]

  def withCaptcha[A](form: Form[A])(implicit ec: scala.concurrent.ExecutionContext): Fu[(Form[A], Captcha)] =
    anyCaptcha map (form -> _)

  import scala.language.reflectiveCalls
  def validateCaptcha(data: CaptchedData) =
    getCaptcha(data.gameId).await(2 seconds, "getCaptcha") valid data.move.trim.toLowerCase

  def captchaFailMessage = Captcha.failMessage
}
