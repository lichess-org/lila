package lila.security

import play.api.libs.ws.StandaloneWSClient
import bloomfilter.mutable.BloomFilter
import akka.stream.scaladsl.*
import scalalib.net.Domain

final class DisposableEmailDomain(
    ws: StandaloneWSClient,
    providerUrl: String
)(using Executor, akka.stream.Materializer):

  import DisposableEmailDomain.*

  private val falsePositiveRate = 0.00003
  private val estimatedCount = 100_000

  private var bloomFilter: BloomFilter[String] =
    BloomFilter[String](100, falsePositiveRate) // temporary empty filter

  private[security] def refresh(): Funit =
    val nextBloom = BloomFilter[String](estimatedCount, falsePositiveRate)
    ws
      .url(providerUrl)
      .stream()
      .flatMap: res =>
        if res.status != 200 then
          fufail(s"Failed to fetch disposable email domains: ${res.status} ${res.statusText}")
        else
          res.bodyAsSource
            .map(_.utf8String)
            .mapConcat(_.linesIterator)
            .runWith:
              Sink.fold[Int, String](0): (nb, domain) =>
                nextBloom.add(domain)
                nb + 1
      .map: nb =>
        bloomFilter.dispose()
        bloomFilter = nextBloom
        lila.mon.email.disposableDomain.update(nb)

  def isDisposable(domain: Domain): Boolean =
    val all = expandDomains(domain)
    !all.exists(DisposableEmailDomain.whitelisted) && all.exists: d =>
      bloomFilter.mightContain(d.lower.value) ||
        domainFragmentRegex.find(d.lower.value)

  // foo.bar.aaa.com -> List(aaa.com, bar.aaa.com, foo.bar.aaa.com)
  private def expandDomains(domain: Domain): List[Domain] =
    def loop(d: Domain): List[Domain] = d.value.indexOf('.') match
      case -1 => Nil
      case i => d :: loop(d.map(_.drop(i + 1)))
    loop(domain)

  def asMxRecord(domain: Domain): Boolean =
    isDisposable(domain) && !mxRecordPasslist(domain.withoutSubdomain)

  def mightBeTypo(domain: Domain): Boolean =
    // gmail.com is very often misspelled
    domain.value.startsWith("g") && scalalib.Levenshtein.isDistanceLessThan(domain.value, "gmail.com", 2)

private object DisposableEmailDomain:

  extension (a: Domain)
    // heuristic to remove user controlled subdomain tails:
    // tail.domain.com, tail.domain.co.uk, tail.domain.edu.au, etc.
    def withoutSubdomainOpt: Option[Domain] =
      a.value.split('.').toList.reverse match
        case tld :: sld :: tail :: _ if sld.lengthIs <= 3 => Domain.from(s"$tail.$sld.$tld")
        case tld :: sld :: _ => Domain.from(s"$sld.$tld")
        case _ => none
    def withoutSubdomain: Domain = withoutSubdomainOpt | a

  def whitelisted(domain: Domain) = whitelist.contains(domain.withoutSubdomain.lower)
  def isOutlook(domain: Domain) = outlookDomains.contains(domain.withoutSubdomain.lower)

  private val mxRecordPasslist =
    Set(Domain("simplelogin.co"), Domain("simplelogin.com"), Domain("anonaddy.me"), Domain("iljmail.com"))

  private val domainFragmentRegex = List(
    "te?mp-?e?mail",
    "e?mail-?te?mp",
    "minutes?-?mail",
    "temporary",
    "throwaway",
    "dispos(e|able)",
    "\bspam\b"
  ).mkString("|").r.unanchored

  private val outlookDomains: Set[Domain.Lower] = Domain.Lower.from:
    Set(
      "outlook.com",
      "outlook.es",
      "outlook.de",
      "outlook.net",
      "outlook.com.au",
      "outlook.com.vn",
      "outlook.com.br",
      "outlook.pt",
      "outlook.sa",
      /* Hotmail (old name)*/
      "hotmail.com",
      "hotmail.co.uk",
      "hotmail.fr",
      "hotmail.de",
      "hotmail.be",
      "hotmail.com.ar",
      "hotmail.es",
      "hotmail.com.br",
      "hotmail.nl",
      /* Live mail */
      "live.com",
      "live.com.mx",
      "live.com.ar",
      "live.com.au",
      "live.co.uk",
      "live.fr",
      "live.com.my",
      "live.com.pt",
      "live.com.sg",
      "live.de",
      "live.be",
      "live.ca",
      "live.nl"
    )

  private val whitelist: Set[Domain.Lower] = outlookDomains ++ Domain.Lower.from:
    Set(
      /* Default domains included */
      "aol.com",
      "att.net",
      "facebook.com",
      "gmail.com",
      "gmx.com",
      "googlemail.com",
      "google.com",
      "ikmail.com",
      "mac.com",
      "me.com",
      "mail.com",
      "msn.com",
      "live.com.org",
      "sbcglobal.net",
      "verizon.net",
      "yahoo.com",
      "yahoo.co.uk",
      "protonmail.com",
      "protonmail.ch",
      "pm.me",
      "proton.me",
      "passfwd.com", // proton.me
      "btopenworld.com",
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
      "pobox.com",
      "rocketmail.com" /* Yahoo */,
      "safe-mail.net",
      "wow.com" /* AOL */,
      "ygm.com" /* AOL */,
      "ymail.com" /* Yahoo */,
      "zoho.com",
      "zoho.in",
      "zohomail.eu",
      "fastmail.com",
      "fastmail.fm",
      "yandex.com",
      "yandex.net",
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
      "laposte.net",
      "yahoo.fr",
      "wanadoo.fr",
      "orange.fr",
      "gmx.fr",
      "sfr.fr",
      "neuf.fr",
      "free.fr",
      /* German ISP domains */
      "aikq.de",
      "gmx.de",
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
      "skynet.be",
      "voo.be",
      "tvcablenet.be",
      "telenet.be",
      /* Argentinian ISP domains */
      "yahoo.com.ar",
      "fibertel.com.ar",
      "speedy.com.ar",
      "arnet.com.ar",
      /* Domains used in Vietnam */
      "yahoo.com.vn",
      "vnnic.vn",
      /* Domains used in Mexico */
      "yahoo.com.mx",
      "prodigy.net.mx",
      /* Domains used in Brazil */
      "yahoo.com.br",
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
      /* Domains used in Portugal */
      "sapo.pt",
      /* Domains without an A record */
      "cabletv.on.ca",
      "unitybox.de",
      "volki.at",
      /* others */
      "skole.hr",
      "freeshell.org",
      "startmail.com",
      "palaciodegranda.com",
      "laudepalaciogranda.com",
      "mozmail.com" // Mozilla Firefox Relay Domain
    )
