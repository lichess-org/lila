package lila.hub

import akka.pattern.ask
import play.api.data._

import actorApi.captcha._
import lila.common.Captcha

trait CaptchedForm {

  import makeTimeout.large

  type CaptchedData = {
    def gameId: String
    def move: String
  }

  def captcher: akka.actor.ActorSelection

  def anyCaptcha: Fu[Captcha] =
    (captcher ? AnyCaptcha).mapTo[Captcha]

  def getCaptcha(id: String): Fu[Captcha] =
    (captcher ? GetCaptcha(id)).mapTo[Captcha]

  def withCaptcha[A](form: Form[A]): Fu[(Form[A], Captcha)] =
    anyCaptcha map (form -> _)

  def validateCaptcha(data: CaptchedData) =
    getCaptcha(data.gameId).await valid data.move.trim.toLowerCase

  val captchaFailMessage = "notACheckmate"
}
