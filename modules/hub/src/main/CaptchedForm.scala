package lila.hub

import lila.common.Captcha
import actorApi.captcha._

import akka.actor.ActorRef
import akka.pattern.ask
import play.api.data._
import play.api.libs.concurrent.Execution.Implicits._

trait CaptchedForm {

  import makeTimeout.large

  type CaptchedData = {
    def gameId: String
    def move: String
  }

  def captcher: ActorRef

  def anyCaptcha: Fu[Captcha] = 
    (captcher ? AnyCaptcha).mapTo[Captcha]

  def getCaptcha(id: String): Fu[Captcha] = 
    (captcher ? GetCaptcha(id)).mapTo[Captcha]

  def withCaptcha[A](form: Form[A]) = anyCaptcha map (form -> _)

  def validateCaptcha(data: CaptchedData) = 
    getCaptcha(data.gameId).await valid data.move.trim.toLowerCase

  val captchaFailMessage = "Not a checkmate"
}
