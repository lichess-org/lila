package lila.i18n

import play.api.libs.json._

import lila.common.PimpedJson._
import play.api.libs.ws.WS
import play.api.Play.current

private[i18n] final class UpstreamFetch(upstreamUrl: Int => String) {

  private type Fetched = Fu[List[Translation]]

  import Translation.translationI18nFormat

  def apply(from: Int): Fetched =
    fetch(upstreamUrl(from)) map parse flatMap {
      _.fold(e => fufail(e.toString), fuccess(_))
    }

  def apply(from: String): Fetched =
    parseIntOption(from).fold(fufail("Bad from argument"): Fetched)(apply)

  private def fetch(url: String): Fu[JsValue] =
    WS.url(url).get() map (_.json)

  private def parse(json: JsValue): JsResult[List[Translation]] =
    Json.fromJson[List[Translation]](json)
}
