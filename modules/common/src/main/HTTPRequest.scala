package lila.common

import play.api.http.HeaderNames
import play.api.libs.typedmap.TypedKey
import play.api.routing.Router
import scalalib.net.{ Bearer, UserAgent }

import scala.util.matching.Regex

import lila.common.Form.trueish
import lila.core.data.UntypedFormRequest
import lila.core.net.*

object HTTPRequest:

  def isXhr(req: Req): Boolean =
    req.headers.get("X-Requested-With").contains("XMLHttpRequest")

  def isSynchronousHttp(req: Req) = !isXhr(req)

  def isSafe(req: Req) = req.method == "GET" || req.method == "HEAD" || req.method == "OPTIONS"
  def isUnsafe(req: Req) = !isSafe(req)

  def isRedirectable(using req: Req) =
    isSynchronousHttp(req) && isSafe(req) && !isLichessMobile

  private val appOrigins: List[Origin] = Origin.from:
    List(
      "capacitor://localhost", // ios
      "ionic://localhost", // ios
      "http://localhost" // android/dev/flutter
    )

  def appOrigin(req: Req): Option[Origin] =
    origin(req).filter: reqOrigin =>
      appOrigins.exists: appOrigin =>
        reqOrigin == appOrigin || reqOrigin.value.startsWith(s"$appOrigin:")

  def isApi(req: Req) = req.path.startsWith("/api/")
  def isApiOrApp(req: Req) = isApi(req) || appOrigin(req).isDefined

  def isAssets(req: Req) = req.path.startsWith("/assets/")

  def userAgent(req: Req): UserAgent =
    req.headers.get(HeaderNames.USER_AGENT).fold(UserAgent.zero)(UserAgent(_))

  val isChrome96Plus = UaMatcher("""Chrome/(?:\d{3,}|9[6-9])""")
  val isChrome113Plus = UaMatcher("""Chrome/(?:11[3-9]|1[2-9]\d)""")
  val isFirefox119Plus = UaMatcher("""Firefox/(?:119|1[2-9]\d)""")
  val isMobileBrowser = UaMatcher("""(?i)iphone|ipad|ipod|android.+mobile""")
  def isLichessMobile(ua: UserAgent): Boolean = ua.value.startsWith("Lichess Mobile/")
  def isLichessMobile(using req: Req): Boolean = isLichessMobile(userAgent(req))
  def isLichobile(ua: UserAgent): Boolean = ua.value.contains("Lichobile/")
  def isLichobile(using req: Req): Boolean = isLichobile(userAgent(req))
  def isAndroid = UaMatcher("Android")
  def lichessMobileVersion(ua: UserAgent): Option[LichessMobileVersion] =
    isLichessMobile(ua).so:
      for
        uaRest <- ua.value.split("/", 2).lift(1)
        versionStr = uaRest.takeWhile(' ' != _)
        version <- versionStr.split('.') match
          case Array(major, minor, _) =>
            (major.toIntOption, minor.toIntOption).mapN(LichessMobileVersion(_, _))
          case _ => none
      yield version

  def origin(req: Req): Option[Origin] = Origin.from(req.headers.get(HeaderNames.ORIGIN))
  def referer(using req: Req): Option[String] = req.headers.get(HeaderNames.REFERER)
  def cookieStr(using req: Req): Option[String] = req.headers.get(HeaderNames.COOKIE)

  def ipAddress(req: Req): IpAddress =
    IpAddress.unchecked(ipAddressStr(req))

  def ipAddressStr(req: Req): String =
    // chain of trusted proxies, strip scope id
    req.remoteAddress.split(", ").last.split("%").head

  def UaMatcher(rStr: String): Req => Boolean =
    val pattern = rStr.r.pattern
    (req: Req) => pattern.matcher(userAgent(req).value).find

  def uaMatches(req: Req, regex: Regex): Boolean = regex.find(userAgent(req).value)

  private val fileExtensionRegex = """\.(?<!^\.)[a-zA-Z0-9]{2,4}$""".r

  def hasFileExtension(req: Req) = fileExtensionRegex.find(req.path)

  def print(using req: Req) = s"${printReq(req)} ${printClient}"

  def printReq(req: Req) = s"${req.method} ${req.domain}${req.uri}"

  def printClient(using req: Req) =
    s"${ipAddressStr(req)} origin:${origin(req).so(_.value)} referer:${~referer} ua:${userAgent(req)}"

  def bearer(using req: Req): Option[(Bearer, Option[String])] = for
    authorization <- req.headers.get(HeaderNames.AUTHORIZATION)
    prefix = "Bearer "
    if authorization.startsWith(prefix)
    raw = authorization.stripPrefix(prefix)
  yield raw.split(':') match
    case Array(bearer, sign) => Bearer(bearer) -> sign.some
    case _ => (Bearer(raw), none)

  def accepts(req: Req): Option[String] = req.headers.get(HeaderNames.ACCEPT)
  def acceptsNdJson(req: Req) = accepts(req) contains "application/x-ndjson"
  def acceptsJson(req: Req) = accepts(req).exists: a =>
    a == "application/web.lichess+json" ||
      a.startsWith("application/json") ||
      a.startsWith("application/vnd.lichess.v")
  def acceptsCsv(req: Req) = accepts(req) contains "text/csv"
  def isEventSource(req: Req): Boolean = accepts(req) contains "text/event-stream"

  def actionName(req: Req): String =
    req.attrs.get(Router.Attrs.ActionName).getOrElse("NoHandler")

  private val LichobileVersionHeaderPattern = """application/vnd\.lichess\.v(\d++)\+json""".r

  def apiVersion(req: Req): Option[ApiVersion] =
    accepts(req).flatMap:
      case LichobileVersionHeaderPattern(v) => ApiVersion.from(v.toIntOption)
      case _ => none

  def queryStringGet(name: String)(using req: Req): Option[String] =
    req.queryString.get(name).flatMap(_.headOption).filter(_.nonEmpty)

  def queryStringBool(name: String)(using req: Req): Boolean =
    queryStringGet(name).exists(trueish)

  def queryStringBoolOpt(name: String)(using req: Req): Option[Boolean] =
    queryStringGet(name).map(trueish)

  def queryStringGetAs[A](name: String)(using req: Req, sr: SameRuntime[String, A]): Option[A] =
    queryStringGet(name).map(sr.apply)

  def fromFormOrQuery(name: String)(using req: UntypedFormRequest): Option[String] =
    req.body.get(name).flatMap(_.headOption).filter(_.nonEmpty).orElse(queryStringGet(name))

  // this header is set by our nginx config, based on the nginx whitelist file.
  def ipTier(req: Req) = IpTier:
    req.headers.get("X-Ip-Tier").exists(_ != "1")

  def isKid(req: Req) =
    req.headers.get("X-Lichess-KidMode").exists(trueish)

  def asn(req: Req): Option[ASN] =
    ASN.from(req.headers.get("X-ASN").flatMap(_.toIntOption))

  def continent(req: Req): Option[Continent] =
    req.headers.get("X-Continent").flatMap(Continent.byCode.get)

  def country(using req: Req): Option[CountryCode] =
    CountryCode.from(req.headers.get("X-Country"))

enum ClientName:
  case lichobile, xhr, mobile, crawler, browser, fishnet, playbot, unknown
  def name = toString
  def isCrawler = this == crawler
  def isFishnet = this == fishnet
  def isHuman = !isCrawler && !isFishnet
  def isMobile = this == mobile
  def isLichobile = this == lichobile
  def isXhr = this == xhr
  def isPlaybot = this == playbot

object ClientName:

  val reqAttr: TypedKey[ClientName] = TypedKey("ClientName")

  def apply(req: Req): ClientName =
    req.attrs.get(reqAttr).getOrElse(unknown)
