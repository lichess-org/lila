package lila
package team

import site.Captcha

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

final class DataForm(captcher: Captcha) {

  import lila.core.Form._

  val create = Form(mapping(
    "name" -> text(minLength = 3, maxLength = 60),
    "location" -> optional(text(minLength = 3, maxLength = 80)),
    "description" -> text(minLength = 60, maxLength = 1000),
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(TeamSetup.apply)(TeamSetup.unapply)
  )

  def createWithCaptcha = create -> captchaCreate

  def captchaCreate: Captcha.Challenge = captcher.create
}

private[team] case class TeamSetup(
    name: String,
    location: Option[String],
    description: String,
    gameId: String,
    move: String) 
