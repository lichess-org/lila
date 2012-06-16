package lila
package i18n

import controllers.routes
import implicits.RichJs._

import java.io.File
import org.eclipse.jgit.api._
import scalaz.effects._
import scala.io.Source
import play.api.libs.json._
import org.joda.time.DateTime

final class UpstreamFetch(
    repoPath: String,
    upstreamDomain: String) {

  def apply(from: Int): IO[Unit] = for {
    response ← fetch(upstreamUrl(from))
    translations = parse(response)
  } yield ()

  private def upstreamUrl(from: Int) =
    "http://" + upstreamDomain + routes.I18n.fetch(from)

  private def fetch(url: String): IO[String] = io {
    Source.fromURL(url).mkString
  }

  private def parse(json: String): List[Translation] = {
    val data: JsValue = Json parse json
    (data.as[List[JsObject]].value map parseTranslation).toList.flatten
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
