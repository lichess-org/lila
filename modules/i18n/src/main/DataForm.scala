package lila.i18n

import lila.db.api.$insert
import tube.translationTube

import play.api.mvc.Request
import play.api.data._
import play.api.data.Forms._
import org.joda.time.DateTime

final class DataForm(
    keys: I18nKeys, 
    val captcher: lila.hub.ActorLazyRef) extends lila.hub.CaptchedForm {

  val translation = Form(mapping(
    "author" -> optional(nonEmptyText),
    "comment" -> optional(nonEmptyText),
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(TransMetadata.apply)(TransMetadata.unapply)
    .verifying(captchaFailMessage, validateCaptcha _))

  def translationWithCaptcha = withCaptcha(translation)

  def process(code: String, metadata: TransMetadata, data: Map[String, String]): Funit = {
    val messages = (data mapValues { msg ⇒
      msg.some map sanitize filter (_.nonEmpty)
    }).toList collect {
      case (key, Some(value)) ⇒ key -> value
    }
    messages.nonEmpty ?? TranslationRepo.nextId flatMap { id ⇒
      val sorted = (keys.keys map { key ⇒
        messages find (_._1 == key.key)
      }).flatten
      val translation = Translation(
        id = id,
        code = code,
        text = sorted map {
          case (key, trans) ⇒ key + "=" + trans
        } mkString "\n",
        author = metadata.author,
        comment = metadata.comment,
        createdAt = DateTime.now)
      $insert(translation)
    }
  }

  def decodeTranslationBody(implicit req: Request[_]): Map[String, String] = req.body match {
    case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined ⇒
      (body.asFormUrlEncoded.get collect {
        case (key, msgs) if key startsWith "key_" ⇒ msgs.headOption map { key.drop(4) -> _ }
      }).flatten.toMap
    case body ⇒ {
      logwarn("Can't parse translation request body: " + body)
      Map.empty
    }
  }

  private def sanitize(message: String) = message.replace("""\n""", " ").trim
}

private[i18n] case class TransMetadata(
  author: Option[String],
  comment: Option[String],
  gameId: String,
  move: String)
