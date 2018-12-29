package lila.security

import play.api.libs.ws.WS
import play.api.Play.current

import lila.common.Domain

final class DisposableEmailDomain(
    providerUrl: String,
    blacklistStr: () => lila.common.Strings,
    bus: lila.common.Bus
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
      bus.publish(
        lila.hub.actorApi.slack.Error(s"Disposable emails list: ${e.getMessage}\nPlease fix $providerUrl"),
        'slack
      )
    }
  }

  def apply(domain: Domain): Boolean =
    !DisposableEmailDomain.whitelisted(domain) && {
      domains.contains(domain.value) ||
        DisposableEmailDomain.staticBlacklist.contains(domain.value) ||
        blacklistStr().value.contains(domain.value)
    }

  def fromDomain(mixedCase: String): Boolean = apply(Domain(mixedCase.toLowerCase))
}

private object DisposableEmailDomain {

  def whitelisted(domain: Domain) = whitelist contains domain.value

  private val staticBlacklist = Set(
    "lichess.org",
    "mybx.site", "mywrld.top", "wemel.top", "matra.top", "dripbank.com", "yopmail.xxi2.com",
    "4bi.email-temp.com", "forevernew.in", "sss.pp.ua", "ttempm.com"
  )

  private val whitelist = Set(
    /* Default domains included */
    "aol.com", "att.net", "comcast.net", "facebook.com", "gmail.com", "gmx.com", "googlemail.com",
    "google.com", "hotmail.com", "hotmail.co.uk", "mac.com", "me.com", "mail.com", "msn.com",
    "live.com", "sbcglobal.net", "verizon.net", "yahoo.com", "yahoo.co.uk", "protonmail.com", "protonmail.ch",

    /* Other global domains */
    "email.com", "games.com" /* AOL */ , "gmx.net", "hush.com", "hushmail.com", "icloud.com", "inbox.com",
    "lavabit.com", "love.com" /* AOL */ , "outlook.com", "pobox.com", "rocketmail.com" /* Yahoo */ ,
    "safe-mail.net", "wow.com" /* AOL */ , "ygm.com" /* AOL */ , "ymail.com" /* Yahoo */ , "zoho.com", "fastmail.fm",
    "yandex.com",

    /* United States ISP domains */
    "bellsouth.net", "charter.net", "comcast.net", "cox.net", "earthlink.net", "juno.com",

    /* British ISP domains */
    "btinternet.com", "virginmedia.com", "blueyonder.co.uk", "freeserve.co.uk", "live.co.uk",
    "ntlworld.com", "o2.co.uk", "orange.net", "sky.com", "talktalk.co.uk", "tiscali.co.uk",
    "virgin.net", "wanadoo.co.uk", "bt.com",

    /* Domains used in Asia */
    "sina.com", "qq.com", "naver.com", "hanmail.net", "daum.net", "nate.com", "yahoo.co.jp", "yahoo.co.kr", "yahoo.co.id", "yahoo.co.in", "yahoo.com.sg", "yahoo.com.ph",

    /* French ISP domains */
    "hotmail.fr", "live.fr", "laposte.net", "yahoo.fr", "wanadoo.fr", "orange.fr", "gmx.fr", "sfr.fr", "neuf.fr", "free.fr",

    /* German ISP domains */
    "gmx.de", "hotmail.de", "live.de", "online.de", "t-online.de" /* T-Mobile */ , "web.de", "yahoo.de",

    /* Russian ISP domains */
    "mail.ru", "rambler.ru", "yandex.ru", "ya.ru", "list.ru",

    /* Belgian ISP domains */
    "hotmail.be", "live.be", "skynet.be", "voo.be", "tvcablenet.be", "telenet.be",

    /* Argentinian ISP domains */
    "hotmail.com.ar", "live.com.ar", "yahoo.com.ar", "fibertel.com.ar", "speedy.com.ar", "arnet.com.ar",

    /* Domains used in Mexico */
    "yahoo.com.mx", "live.com.mx", "hotmail.es", "hotmail.com.mx", "prodigy.net.mx",

    /* Domains used in Brazil */
    "yahoo.com.br", "hotmail.com.br", "outlook.com.br", "uol.com.br", "bol.com.br", "terra.com.br", "ig.com.br", "itelefonica.com.br", "r7.com", "zipmail.com.br", "globo.com", "globomail.com", "oi.com.br"
  )
}
