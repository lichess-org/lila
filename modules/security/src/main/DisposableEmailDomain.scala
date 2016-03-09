package lila.security

import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current
import kamon.Kamon

final class DisposableEmailDomain(
    providerUrl: String,
    busOption: Option[lila.common.Bus]) {

  private type Matcher = String => Boolean

  private var matchers = List.empty[Matcher]

  private[security] def refresh {
    WS.url(providerUrl).get() map { res =>
      setDomains(res.json)
    } recover {
      case _: java.net.ConnectException => // ignore network errors
      case e: Exception                 => onError(e)
    }
  }

  private[security] def setDomains(json: JsValue): Unit = try {
    val ds = json.as[List[String]]
    if (ds.size != matchers.size)
      loginfo(s"[disposable email] registered ${matchers.size} -> ${ds.size} domains")
    matchers = ds.map { d =>
      val regex = s"""(.+\\.|)${d.replace(".", "\\.")}"""
      makeMatcher(regex)
    }
    Kamon.metrics.histogram("security.disposable_email_domains") record matchers.size
    failed = false
  }
  catch {
    case e: Exception => onError(e)
  }

  private var failed = false

  private def onError(e: Exception) {
    logerr(s"Can't update disposable emails: $e")
    if (!failed) {
      failed = true
      busOption.foreach { bus =>
        bus.publish(
          lila.hub.actorApi.slack.Error(s"Disposable emails list: ${e.getMessage}\nPlease fix $providerUrl"),
          'slack)
      }
    }
  }

  private def makeMatcher(regex: String): Matcher = {
    val matcher = regex.r.pattern matcher _
    (s: String) => matcher(s).matches
  }

  def apply(domain: String) = matchers exists { _(domain) }
}
