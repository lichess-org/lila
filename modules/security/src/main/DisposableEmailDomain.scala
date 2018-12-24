package lila.security

import play.api.libs.ws.WS
import play.api.Play.current

final class DisposableEmailDomain(
    providerUrl: String,
    blacklistStr: () => lila.common.Strings,
    busOption: Option[lila.common.Bus]
) {

  private var domains = Set.empty[String]
  private var failed = false

  private[security] def refresh: Unit = {
    WS.url(providerUrl).get() map { res =>
      domains = res.body.lines.toSet
      failed = domains.size < 10000
      lila.mon.email.disposableDomain(domains.size)
    } recover {
      case _: java.net.ConnectException => // ignore network errors
      case e: Exception => onError(e)
    }
  }

  private def onError(e: Exception): Unit = {
    logger.error("Can't update disposable emails", e)
    if (!failed) {
      failed = true
      busOption.foreach { bus =>
        bus.publish(
          lila.hub.actorApi.slack.Error(s"Disposable emails list: ${e.getMessage}\nPlease fix $providerUrl"),
          'slack
        )
      }
    }
  }

  def apply(mixedCase: String) = {
    val domain = mixedCase.toLowerCase
    !DnsCheck.mainstreamDomains(domain) && {
      domains.contains(domain) || blacklistStr().value.contains(domain)
    }
  }
}
