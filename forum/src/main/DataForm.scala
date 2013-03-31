package lila.forum

import lila.common.Captcha
import lila.hub.actorApi.captcha._

import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor.ActorRef
import akka.pattern.ask

final class DataForm(captcher: ActorRef) {

  import DataForm._

  private implicit val timeout = makeTimeout.large

  val postMapping = mapping(
    "text" -> text(minLength = 3),
    "author" -> optional(text),
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(PostData.apply)(PostData.unapply).verifying(
    "Not a checkmate", 
    data ⇒ getCaptcha(data.gameId).await valid data.move.trim.toLowerCase
  )

  val post = Form(postMapping)

  def postWithCaptcha = anyCaptcha map (post -> _)

  val topic = Form(mapping(
    "name" -> text(minLength = 3),
    "post" -> postMapping
  )(TopicData.apply)(TopicData.unapply))

  def anyCaptcha: Fu[Captcha] = 
    (captcher ? AnyCaptcha).mapTo[Captcha]
  def getCaptcha(id: String): Fu[Captcha] = 
    (captcher ? GetCaptcha(id)).mapTo[Captcha]
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
