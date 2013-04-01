package lila.i18n

import lila.common.PimpedJson._

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.Implicits._

import org.joda.time.DateTime

private[i18n] final class UpstreamFetch(upstreamUrl: Int ⇒ String) {

  private type Fetched = Fu[List[Translation]]

  def apply(from: Int): Fetched =
    fetch(upstreamUrl(from)) map parse

  def apply(from: String): Fetched =
    parseIntOption(from).fold(fufail("Bad from argument"): Fetched)(apply)

  // private def upstreamUrl(from: Int) =
  //   "http://" + upstreamDomain + routes.I18n.fetch(from)

  private def fetch(url: String): Fu[JsValue] =
    WS.url(url).get() map (_.json)

  // this function signature is lying.
  private def parse(json: JsValue): List[Translation] =
    (json.as[List[JsObject]].value map parseTranslation).toList map {
      _ err "Received broken JSON from upstream"
    }

  private def parseTranslation(js: JsObject): Option[Translation] = for {
    id ← js int "id"
    code ← js str "code"
    text ← js str "text"
    author = js str "author"
    comment = js str "comment"
    ts ← js long "createdAt"
  } yield Translation(
    id = id,
    code = code,
    text = text,
    author = author,
    comment = comment,
    createdAt = new DateTime(ts))
}
