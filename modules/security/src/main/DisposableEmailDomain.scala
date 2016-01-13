package lila.security

import play.api.libs.ws.WS
import play.api.Play.current

final class DisposableEmailDomain(providerUrl: String) {

  private var domains = List.empty[String]

  private[security] def refresh {
    WS.url(providerUrl).get() map { res =>
      try {
        val prevSize = domains.size
        domains = res.json.as[List[String]]
        if (domains.size != prevSize)
          loginfo(s"[disposable email] registered ${domains.size} domains")
      }
      catch {
        case e: Exception => logerr(s"Can't update disposable emails: $e")
      }
    }
  }

  def apply(domain: String) = domains.exists { d =>
    domain == d || domain.endsWith(s".$d")
  }
}
