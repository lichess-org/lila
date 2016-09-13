package lila.security

import play.api.libs.ws.WS
import play.api.Play.current

final class DisposableEmailDomain(
    providerUrl: String,
    busOption: Option[lila.common.Bus]) {

  private type Matcher = String => Boolean

  private var matchers = List.empty[Matcher]

  private[security] def refresh {
    WS.url(providerUrl).get() map { res =>
      setDomains(textToDomains(res.body))
      lila.mon.email.disposableDomain(matchers.size)
    } recover {
      case _: java.net.ConnectException => // ignore network errors
      case e: Exception                 => onError(e)
    }
  }

  private[security] def setDomains(domains: List[String]): Unit = try {
    matchers = domains.map { d =>
      val regex = s"""(.+\\.|)${d.replace(".", "\\.")}"""
      makeMatcher(regex)
    }
    failed = false
  }
  catch {
    case e: Exception => onError(e)
  }

  private[security] def textToDomains(text: String): List[String] =
    text.lines.map(_.trim).filter(_.nonEmpty).toList

  private var failed = false

  private def onError(e: Exception) {
    logger.error("Can't update disposable emails", e)
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
