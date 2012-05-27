package lila
package forum

import site.Captcha

import play.api.data._
import play.api.data.Forms._

final class DataForm(captcher: Captcha) {

  import DataForm._

  def postMapping = mapping(
    "text" -> text(minLength = 3),
    "author" -> optional(text),
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(PostData.apply)(PostData.unapply).verifying(
    "Not a checkmate", 
    data ⇒ captcher get data.gameId valid data.move.trim.toLowerCase
  )

  def post = Form(postMapping)

  def postWithCaptcha = post -> captchaCreate

  def topic = Form(mapping(
    "name" -> text(minLength = 3),
    "post" -> postMapping
  )(TopicData.apply)(TopicData.unapply))

  def captchaCreate: Captcha.Challenge = captcher.create
}

object DataForm {

  case class PostData(
    text: String,
    author: Option[String],
    gameId: String,
    move: String)

  case class TopicData(
    name: String,
    post: PostData)
}
