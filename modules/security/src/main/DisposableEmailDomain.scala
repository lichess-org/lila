package lila.security

import play.api.libs.ws.StandaloneWSClient

import lila.common.Domain

final class DisposableEmailDomain(
    ws: StandaloneWSClient,
    providerUrl: String,
    checkMailBlocked: () => Fu[List[String]]
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val staticRegex = toRegexStr(DisposableEmailDomain.staticBlacklist.iterator)

  private var regex = finalizeRegex(staticRegex)

  private[security] def refresh(): Unit =
    for {
      blacklist <- ws.url(providerUrl).get().map(_.body.linesIterator) recover { case e: Exception =>
        logger.warn("DisposableEmailDomain.refresh", e)
        Iterator.empty
      }
      checked <- checkMailBlocked()
    } {
      val regexStr  = s"${toRegexStr(blacklist)}|${toRegexStr(checked.iterator)}"
      val nbDomains = regexStr.count('|' ==)
      lila.mon.email.disposableDomain.update(nbDomains)
      regex = finalizeRegex(s"$staticRegex|$regexStr")
    }

  private def toRegexStr(domains: Iterator[String]) = domains.map(l => l.replace(".", "\\.")).mkString("|")

  private def finalizeRegex(regexStr: String) = s"(^|\\.)($regexStr)$$".r

  def apply(domain: Domain): Boolean = {
    val lower = domain.lower
    !DisposableEmailDomain.whitelisted(lower) && regex.find(lower.value)
  }

  def isOk(domain: Domain) = !apply(domain)

  def fromDomain(mixedCase: String): Boolean = apply(Domain(mixedCase.toLowerCase))
}

private object DisposableEmailDomain {

  def whitelisted(domain: Domain.Lower) = whitelist contains domain.value

  private val staticBlacklist = Set(
    "lichess.org",
    "gamil.com",
    "gmali.com"
  )

  private val whitelist = Set(
    "fide.com", // https://check-mail.org/domain/fide.com/ says DISPOSABLE / TEMPORARY DOMAIN
    /* Default domains included */
    "aol.com",
    "att.net",
    "facebook.com",
    "gmail.com",
    "gmx.com",
    "googlemail.com",
    "google.com",
    "hotmail.com",
    "hotmail.co.uk",
    "mac.com",
    "me.com",
    "mail.com",
    "msn.com",
    "live.com",
    "sbcglobal.net",
    "verizon.net",
    "yahoo.com",
    "yahoo.co.uk",
    "protonmail.com",
    "protonmail.ch",
    "pm.me",
    /* Other global domains */
    "email.com",
    "games.com" /* AOL */,
    "gmx.net",
    "hush.com",
    "hushmail.com",
    "icloud.com",
    "inbox.com",
    "lavabit.com",
    "love.com" /* AOL */,
    "outlook.com",
    "pobox.com",
    "rocketmail.com" /* Yahoo */,
    "safe-mail.net",
    "wow.com" /* AOL */,
    "ygm.com" /* AOL */,
    "ymail.com" /* Yahoo */,
    "zoho.com",
    "fastmail.com",
    "fastmail.fm",
    "yandex.com",
    /* United States ISP domains */
    "bellsouth.net",
    "charter.net",
    "comcast.net",
    "cox.net",
    "earthlink.net",
    "juno.com",
    /* Canadian ISP domains */
    "yahoo.ca",
    /* British ISP domains */
    "btinternet.com",
    "virginmedia.com",
    "blueyonder.co.uk",
    "freeserve.co.uk",
    "live.co.uk",
    "ntlworld.com",
    "o2.co.uk",
    "orange.net",
    "sky.com",
    "talktalk.co.uk",
    "tiscali.co.uk",
    "virgin.net",
    "wanadoo.co.uk",
    "bt.com",
    /* Domains used in Asia */
    "sina.com",
    "qq.com",
    "naver.com",
    "hanmail.net",
    "daum.net",
    "nate.com",
    "yahoo.co.jp",
    "yahoo.co.kr",
    "yahoo.co.id",
    "yahoo.co.in",
    "yahoo.com.sg",
    "yahoo.com.ph",
    /* French ISP domains */
    "hotmail.fr",
    "live.fr",
    "laposte.net",
    "yahoo.fr",
    "wanadoo.fr",
    "orange.fr",
    "gmx.fr",
    "sfr.fr",
    "neuf.fr",
    "free.fr",
    /* German ISP domains */
    "gmx.de",
    "hotmail.de",
    "live.de",
    "online.de",
    "t-online.de" /* T-Mobile */,
    "web.de",
    "yahoo.de",
    /* Russian ISP domains */
    "mail.ru",
    "rambler.ru",
    "yandex.ru",
    "ya.ru",
    "list.ru",
    /* Belgian ISP domains */
    "hotmail.be",
    "live.be",
    "skynet.be",
    "voo.be",
    "tvcablenet.be",
    "telenet.be",
    /* Argentinian ISP domains */
    "hotmail.com.ar",
    "live.com.ar",
    "yahoo.com.ar",
    "fibertel.com.ar",
    "speedy.com.ar",
    "arnet.com.ar",
    /* Domains used in Mexico */
    "yahoo.com.mx",
    "live.com.mx",
    "hotmail.es",
    "hotmail.com.mx",
    "prodigy.net.mx",
    /* Domains used in Brazil */
    "yahoo.com.br",
    "hotmail.com.br",
    "outlook.com.br",
    "uol.com.br",
    "bol.com.br",
    "terra.com.br",
    "ig.com.br",
    "itelefonica.com.br",
    "r7.com",
    "zipmail.com.br",
    "globo.com",
    "globomail.com",
    "oi.com.br",
    /* Domains without an A record */
    "cabletv.on.ca",
    "live.ca",
    "unitybox.de",
    "volki.at",
    /* others */
    "skole.hr"
  )
}
