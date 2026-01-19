package lila.web

import play.api.libs.json.{ JsArray, Json }
import play.api.mvc.RequestHeader

import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.config.NetConfig

object StaticContent:

  val robotsTxt = """User-agent: *
Allow: /
Disallow: /game/export/
Disallow: /games/export/
Disallow: /api/
Disallow: /opening/config/
Allow: /game/export/gif/thumbnail/
"""

  def manifest(net: NetConfig) =
    Json.obj(
      "name" -> net.domain,
      "short_name" -> "Lichess",
      "start_url" -> "/",
      "display" -> "standalone",
      "background_color" -> "#161512",
      "theme_color" -> "#161512",
      "description" -> "The (really) free, no-ads, open source chess server.",
      "icons" -> List(32, 64, 128, 192, 256, 512, 1024).map: size =>
        Json.obj(
          "src" -> s"//${net.assetDomain}/assets/logo/lichess-favicon-$size.png",
          "sizes" -> s"${size}x$size",
          "type" -> "image/png"
        ),
      "related_applications" -> Json.arr(
        Json.obj(
          "platform" -> "play",
          "url" -> mobileAndroidUrl,
          "id" -> mobileAndroidId
        ),
        Json.obj(
          "platform" -> "itunes",
          "url" -> mobileIosUrl
        ),
        Json.obj(
          "platform" -> "ios",
          "url" -> mobileIosUrl
        )
      )
    )

  val mobileAndroidId = "https://play.google.com/store/apps/details?id=org.lichess.mobileV2"
  val mobileAndroidUrl = s"https://play.google.com/store/apps/details?id=$mobileAndroidId"
  val mobileIosUrl = "https://apps.apple.com/app/lichess/id1662361230"

  def appStoreUrl(using req: RequestHeader) =
    if HTTPRequest.isAndroid(req) then mobileAndroidUrl else mobileIosUrl

  val swagStoreTlds = Map(
    "US" -> "com",
    "CA" -> "ca",
    "DE" -> "de",
    "FR" -> "fr",
    "UK" -> "co.uk",
    "IT" -> "it",
    "ES" -> "es",
    "NL" -> "nl",
    "PL" -> "pl",
    "BE" -> "be",
    "DK" -> "dk",
    "AU" -> "com.au",
    "IE" -> "ie",
    "NO" -> "no",
    "CH" -> "ch",
    "FI" -> "fi",
    "SE" -> "se",
    "AT" -> "at"
  )
  def swagUrl(countryCode: Option[String]) =
    val tld = swagStoreTlds.getOrElse(~countryCode, "net")
    s"https://lichess.myspreadshop.$tld/"

  val variantsJson =
    JsArray(chess.variant.Variant.list.all.map { v =>
      Json.obj(
        "id" -> v.id,
        "key" -> v.key,
        "name" -> v.name
      )
    })

  val externalLinks = Map(
    "mastodon" -> "https://mastodon.online/@lichess",
    "github" -> "https://github.com/lichess-org",
    "discord" -> "https://discord.gg/lichess",
    "bluesky" -> "https://bsky.app/profile/lichess.org",
    "youtube" -> "https://youtube.com/@LichessDotOrg",
    "twitch" -> "https://www.twitch.tv/lichessdotorg"
  )

  def legacyQaQuestion(id: Int) =
    val faq = routes.Main.faq.url
    id match
      case 103 => s"$faq#acpl"
      case 258 => s"$faq#marks"
      case 13 => s"$faq#titles"
      case 87 => routes.User.ratingDistribution(PerfKey.blitz).url
      case 110 => s"$faq#name"
      case 29 => s"$faq#titles"
      case 4811 => s"$faq#lm"
      case 216 => routes.Main.mobile.url
      case 340 => s"$faq#trophies"
      case 6 => s"$faq#ratings"
      case 207 => s"$faq#hide-ratings"
      case 547 => s"$faq#leaving"
      case 259 => s"$faq#trophies"
      case 342 => s"$faq#provisional"
      case 50 => routes.Cms.help.url
      case 46 => s"$faq#name"
      case 122 => s"$faq#marks"
      case _ => faq
