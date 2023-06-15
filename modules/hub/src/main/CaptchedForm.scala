package lila.hub

import akka.pattern.ask
import play.api.data.*

import actorApi.captcha.*
import lila.common.Captcha

trait CaptchedForm:

  import makeTimeout.long

  type CaptchedData = {
    def gameId: GameId
    def move: String
  }

  val captcher: actors.Captcher

  def anyCaptcha: Fu[Captcha] =
    (captcher.actor ? AnyCaptcha).mapTo[Captcha]

  def getCaptcha(id: GameId): Fu[Captcha] =
    (captcher.actor ? GetCaptcha(id)).mapTo[Captcha]

  def withCaptcha[A](form: Form[A])(using Executor): Fu[(Form[A], Captcha)] =
    anyCaptcha map (form -> _)

  import scala.reflect.Selectable.reflectiveSelectable
  def validateCaptcha(data: CaptchedData) =
    getCaptcha(data.gameId).await(2 seconds, "getCaptcha") valid data.move.trim.toLowerCase

  def captchaFailMessage = Captcha.failMessage
