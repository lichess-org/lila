package lila.security

import old.play.api.libs.ws.WS

final class DisposableEmailDomain(
    providerUrl: String,
    busOption: Option[lila.common.Bus]
) {

  private type Matcher = String => Boolean

  private var matchers = List.empty[Matcher]

  private[security] def refresh {
    WS.url(providerUrl).get() map { res =>
      setDomains(textToDomains(res.body))
      lila.mon.email.disposableDomain(matchers.size)
    } recover {
      case _: java.net.ConnectException => // ignore network errors
      case e: Exception => onError(e)
    }
  }

  private[security] def setDomains(domains: List[String]): Unit = try {
    matchers = ("lichess.org" :: domains).map { d =>
      val r = d.replace("\\w", "[\\w-]").replace(".", "\\.")
      val regex = s"""(.+\\.|)$r(\\..+)?"""
      makeMatcher(regex)
    }
    failed = false
  } catch {
    case e: Exception => onError(e)
  }

  private[security] def textToDomains(text: String): List[String] =
    text.lines.map(_.trim.toLowerCase).filter(_.nonEmpty).toList

  private var failed = false

  private def onError(e: Exception) {
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

  private def makeMatcher(regex: String): Matcher = {
    val matcher = regex.r.pattern matcher _
    (s: String) => matcher(s).matches
  }

  def isMainstream(domain: String) =
    DisposableEmailDomain.mainstreamDomains contains domain.toLowerCase

  def apply(domain: String) =
    if (isMainstream(domain)) false
    else matchers exists { _(domain.toLowerCase) }
}

object DisposableEmailDomain {

  val mainstreamDomains = Set(
    /* Default domains included */
    "aol.com", "att.net", "comcast.net", "facebook.com", "gmail.com", "gmx.com", "googlemail.com",
    "google.com", "hotmail.com", "hotmail.co.uk", "mac.com", "me.com", "mail.com", "msn.com",
    "live.com", "sbcglobal.net", "verizon.net", "yahoo.com", "yahoo.co.uk",

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
