package lila.common

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current

// http://detectlanguage.com
final class DetectLanguage(url: String, key: String) {

  private case class Detection(
      language: String,
      confidence: Float,
      isReliable: Boolean
  )

  private implicit val DetectionReads = Json.reads[Detection]

  private val messageMaxLength = 2000

  private val defaultLang = Lang("en")

  def apply(message: String): Fu[Option[Lang]] =
    if (key.isEmpty) fuccess(defaultLang.some)
    else WS.url(url).post(Map(
      "key" -> Seq(key),
      "q" -> Seq(message take messageMaxLength)
    )) map { response =>
      (response.json \ "data" \ "detections").asOpt[List[Detection]] match {
        case None =>
          lila.log("DetectLanguage").warn(s"Invalide service response ${response.json}")
          None
        case Some(res) => res.filter(_.isReliable)
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

  import com.typesafe.config.Config
  def apply(config: Config): DetectLanguage = new DetectLanguage(
    url = config getString "api.url",
    key = config getString "api.key"
  )
}
