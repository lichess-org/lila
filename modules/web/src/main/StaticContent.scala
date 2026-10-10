package lila.web

import play.api.libs.json.{ JsArray, Json }
import play.api.mvc.RequestHeader

import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.config.NetConfig

final class StaticContent(net: NetConfig):

  import StaticContent.*

  val robotsTxt = """User-agent: *
Allow: /
Disallow: /game/
Disallow: /games/export/
Disallow: /api/
Disallow: /opening/config/
Disallow: /study/search
Disallow: /study/embed/
Disallow: /embed/
Disallow: /video?*
Disallow: /training/of-player
Allow: /game/export/gif/thumbnail/
"""

  val manifest = Json.obj(
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

  val variantsJson = JsArray:
    chess.variant.Variant.list.all.map: v =>
      Json.obj(
        "id" -> v.id,
        "key" -> v.key,
        "name" -> v.name
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
      case 216 => routes.Main.app.url
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

  val organizationScript = lila.ui.bits.structuredData("Organization"):
    Json.obj(
      "url" -> net.baseUrl,
      "sameAs" -> List(
        "https://www.wikidata.org/wiki/Q19831807",
        "https://en.wikipedia.org/wiki/Lichess", // only one WP language version is needed, the rest can be discovered from Wikidata
        "https://github.com/lichess-org",
        "https://crowdin.com/translate/lichess",
        "https://mastodon.online/@lichess",
        "https://bsky.app/profile/lichess.org",
        "https://www.instagram.com/lichessdotorg",
        "https://www.facebook.com/lichessdotorg/",
        "https://youtube.com/@LichessDotOrg",
        "https://www.twitch.tv/lichessdotorg",
        "https://discord.gg/lichess",
        "https://www.reddit.com/r/lichessdotorg/",
        "https://telegram.me/lichessdotorg",
        "https://whatsapp.com/channel/0029VaDXYlD2f3ENRMZQHF0K",
        "https://www.tiktok.com/@lichessdotorg",
        "https://www.linkedin.com/company/lichess",
        "https://www.threads.net/@lichessdotorg",
        "https://huggingface.co/Lichess",
        "https://www.kaggle.com/organizations/lichess"
      ),
      "logo" -> s"https://${net.assetDomain}/assets/logo/lichess.svg",
      "name" -> "Lichess",
      "legalName" -> "LICHESS.ORG",
      "description" -> "The (really) free, no-ads, open source chess server.", // should be kept in English unless /about receives more localisations
      "email" -> "contact@lichess.org",
      "foundingDate" -> "2010-06-20",
      "taxID" -> "830378717", // SIREN
      "iso6523Code" -> "0009:830378717" // French ICD:SIREN
    )

object StaticContent:

  val mobileAndroidId = "org.lichess.mobileV2"
  val mobileAndroidUrl = s"https://play.google.com/store/apps/details?id=$mobileAndroidId"
  val mobileIosUrl = "https://apps.apple.com/app/lichess/id1662361230"
  val mobileFdroidUrl = s"https://f-droid.org/packages/$mobileAndroidId"
