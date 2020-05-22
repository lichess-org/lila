package lila.common

import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.i18n.Lang
import io.methvin.play.autoconfig._
import scala.math.Ordering.Float.TotalOrdering

import lila.common.config.Secret

// http://detectlanguage.com
final class DetectLanguage(
    ws: WSClient,
    config: DetectLanguage.Config
)(implicit ec: scala.concurrent.ExecutionContext) {

  import DetectLanguage.Detection

  implicit private val DetectionReads = Json.reads[Detection]

  private val messageMaxLength = 2000

  private val defaultLang = Lang("en")

  def apply(message: String): Fu[Option[Lang]] =
    if (config.key.value.isEmpty) fuccess(defaultLang.some)
    else
      ws.url(config.url)
        .post(
          Map(
            "key" -> config.key.value,
            "q"   -> message.take(messageMaxLength)
          )
        ) map { response =>
        (response.json \ "data" \ "detections").asOpt[List[Detection]] match {
          case None =>
            lila.log("DetectLanguage").warn(s"Invalide service response ${response.json}")
            None
          case Some(res) =>
            res
              .filter(_.isReliable)
              .sortBy(-_.confidence)
              .headOption map (_.language) flatMap Lang.get
        }
      } recover {
        case e: Exception =>
          lila.log("DetectLanguage").warn(e.getMessage, e)
          defaultLang.some
      }
}

object DetectLanguage {

  final class Config(val url: String, val key: Secret)
  implicit val configLoader = AutoConfig.loader[Config]

  final private case class Detection(
      language: String,
      confidence: Float,
      isReliable: Boolean
  )
}
