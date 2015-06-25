package lila.security

import play.api.libs.ws.WS
import play.api.Play.current

final class DisposableEmail(providerUrl: String) {

  private var domains = Set[String]()

  private[security] def refresh {
    WS.url(providerUrl).get() map { res =>
      domains = res.json.as[Set[String]]
      loginfo(s"[disposable email] registered ${domains.size} domains")
    }
  }

  def isDisposable(email: String) = domainOf(email) ?? domains.contains

  private def domainOf(email: String) = email split '@' lift 1
}
