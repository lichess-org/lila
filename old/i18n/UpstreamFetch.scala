package lila.app
package i18n

import controllers.routes
import implicits.RichJs._

import scalaz.effects._
import scala.io.Source
import play.api.libs.json._
import org.joda.time.DateTime

private[i18n] final class UpstreamFetch(upstreamDomain: String) {

  def apply(from: Int): IO[List[Translation]] = 
    fetch(upstreamUrl(from)) map parse

  private def upstreamUrl(from: Int) =
    "http://" + upstreamDomain + routes.I18n.fetch(from)

  private def fetch(url: String): IO[String] = io {
    Source.fromURL(url).mkString
  }

  private def parse(json: String): List[Translation] = {
    val data: JsValue = Json parse json
    val options = (data.as[List[JsObject]].value map parseTranslation).toList
    options map (_ err "Received broken JSON from upstream")
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
