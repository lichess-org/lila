package lila.security

import play.api.libs.ws.WS
import play.api.Play.current

final class DisposableEmailDomain(providerUrl: String) {

  private var domains = List.empty[String]

  private[security] def refresh {
    WS.url(providerUrl).get() map { res =>
      domains = res.json.as[List[String]]
      loginfo(s"[disposable email] registered ${domains.size} domains")
    }
  }

  def apply(domain: String) = domains.exists { d =>
    domain == d || domain.endsWith(s".$d")
  }
}
