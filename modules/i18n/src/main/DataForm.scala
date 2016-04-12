package lila.i18n

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Request

final class DataForm(
    repo: TranslationRepo,
    keys: I18nKeys,
    val captcher: akka.actor.ActorSelection,
    callApi: CallApi) extends lila.hub.CaptchedForm {

  val translation = Form(mapping(
    "comment" -> optional(nonEmptyText),
    "gameId" -> nonEmptyText,
    "move" -> nonEmptyText
  )(TransMetadata.apply)(TransMetadata.unapply)
    .verifying(captchaFailMessage, validateCaptcha _))

  def translationWithCaptcha = withCaptcha(translation)

  def process(code: String, metadata: TransMetadata, data: Map[String, String], user: String): Funit = {
    val messages = (data mapValues { msg =>
      msg.some map sanitize filter (_.nonEmpty)
    }).toList collect {
      case (key, Some(value)) => key -> value
    }
    messages.nonEmpty ?? repo.nextId flatMap { id =>
      val sorted = (keys.keys map { key =>
        messages find (_._1 == key.key)
      }).flatten
      val translation = Translation(
        _id = id,
        code = code,
        text = sorted map {
          case (key, trans) => key + "=" + trans
        } mkString "\n",
        comment = metadata.comment,
        author = user.some,
        createdAt = DateTime.now)
      repo.insert(translation).void >>- callApi.submit(code)
    }
  }

  def decodeTranslationBody(implicit req: Request[_]): Map[String, String] = req.body match {
    case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined =>
      (body.asFormUrlEncoded.get collect {
        case (key, msgs) if key startsWith "key_" => msgs.headOption map { key.drop(4) -> _ }
      }).flatten.toMap
    case body => {
      logger.warn("Can't parse translation request body: " + body)
      Map.empty
    }
  }

  private def sanitize(message: String) = message.replace("""\n""", " ").trim
}

private[i18n] case class TransMetadata(
  comment: Option[String],
  gameId: String,
  move: String)
