package lila
package i18n

import play.api.mvc.Request
import play.api.data._
import play.api.data.Forms._
import scalaz.effects._

import site.Captcha

final class DataForm(
    repo: TranslationRepo,
    keys: I18nKeys,
    captcher: Captcha) {

  val translation = Form(mapping(
    "author" -> optional(nonEmptyText),
    "comment" -> optional(nonEmptyText),
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(TransMetadata.apply)(TransMetadata.unapply).verifying(
      "Not a checkmate",
      data ⇒ captcher get data.gameId valid data.move.trim.toLowerCase
    ))

  def translationWithCaptcha = translation -> captchaCreate

  def captchaCreate: Captcha.Challenge = captcher.create

  def process(code: String, metadata: TransMetadata, data: Map[String, String]): IO[Unit] = {
    val messages = (data mapValues { msg ⇒
      msg.some map sanitize filter (_.nonEmpty)
    }).flatten.toList
    val sorted = (keys.keys map { key ⇒
      messages find (_._1 == key.key)
    }).flatten
    messages.nonEmpty.fold(
      for {
        id ← repo.nextId
        translation = Translation(
          id = id,
          code = code,
          text = sorted map {
            case (key, trans) ⇒ key + "=" + trans
          } mkString "\n",
          author = metadata.author,
          comment = metadata.comment)
        _ ← repo insertIO translation
      } yield (),
      io()
    )
  }

  def decodeTranslationBody(implicit req: Request[_]): Map[String, String] = req.body match {
    case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined ⇒
      (body.asFormUrlEncoded.get collect {
        case (key, msgs) if key startsWith "key_" ⇒ msgs.headOption map { key.drop(4) -> _ }
      }).flatten.toMap
    case body ⇒ {
      println("Can't parse translation request body: " + body)
      Map.empty
    }
  }

  private def sanitize(message: String) = message.replace("""\n""", " ").trim
}

case class TransMetadata(
  author: Option[String],
  comment: Option[String],
  gameId: String,
  move: String)
