package lila.forum

import play.api.ConfigLoader
import play.api.i18n.Lang
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import scala.math.Ordering.Float.TotalOrdering

import lila.common.autoconfig.*
import lila.common.config.given
import lila.core.config.Secret

// http://detectlanguage.com
final class DetectLanguage(
    ws: StandaloneWSClient,
    config: DetectLanguage.Config
)(using Executor):

  import DetectLanguage.Detection

  private given Reads[Detection] = Json.reads

  private val messageMaxLength = 2000

  private val defaultLang = Lang("en")

  def apply(message: String): Fu[Option[Lang]] =
    if config.key.value.isEmpty then fuccess(defaultLang.some)
    else
      ws.url(config.url)
        .post(
          Map(
            "key" -> config.key.value,
            "q" -> message.take(messageMaxLength)
          )
        )
        .map { response =>
          (response.body[JsValue] \ "data" \ "detections").asOpt[List[Detection]] match
            case None =>
              lila.log("DetectLanguage").warn(s"Invalide service response ${response.body[JsValue]}")
              None
            case Some(res) =>
              res
                .filter(_.isReliable)
                .sortBy(-_.confidence)
                .headOption
                .map(_.language)
                .flatMap(Lang.get)
        }
        .recover { case e: Exception =>
          lila.log("DetectLanguage").warn(e.getMessage, e)
          defaultLang.some
        }

object DetectLanguage:

  final class Config(val url: String, val key: Secret)
  given ConfigLoader[Config] = AutoConfig.loader

  final private case class Detection(
      language: String,
      confidence: Float,
      isReliable: Boolean
  )
