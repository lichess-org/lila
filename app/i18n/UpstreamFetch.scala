package lila
package i18n

import controllers.routes
import implicits.RichJs._

import java.io.File
import org.eclipse.jgit.api._
import scalaz.effects._
import play.api.libs.ws.WS
import play.api.libs.json._

final class UpstreamFetch(
    repoPath: String,
    upstreamDomain: String) {

  def apply(from: Int): IO[Unit] = io {
    WS.url(upstreamUrl(from)).get() onRedeem { translations =>
    //response.json.asOpt[JsObject]
      println("got them")
    }
  }

  private def upstreamUrl(from: Int) =
    "http://" + upstreamDomain + routes.I18n.fetch(from)

}
