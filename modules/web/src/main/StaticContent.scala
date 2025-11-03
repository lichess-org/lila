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
          "url" -> "https://play.google.com/store/apps/details?id=org.lichess.mobileapp"
        ),
        Json.obj(
          "platform" -> "itunes",
          "url" -> "https://itunes.apple.com/us/app/lichess-free-online-chess/id968371784"
        )
      )
    )

  val mobileAndroidUrl = "https://play.google.com/store/apps/details?id=org.lichess.mobileV2"
  val mobileIosUrl = "https://apps.apple.com/app/lichess/id1662361230"

  def appStoreUrl(using req: RequestHeader) =
    if HTTPRequest.isAndroid(req) then mobileAndroidUrl else mobileIosUrl

  def swagUrlMaker(tld: String) = s"https://lichess.myspreadshop.$tld/"
  def swagUrl(countryCode: Option[String]) =
    countryCode match
      case Some("US") => swagUrlMaker("com")
      case Some("CA") => swagUrlMaker("ca")
      case Some("DE") => swagUrlMaker("de")
      case Some("FR") => swagUrlMaker("fr")
      case Some("UK") => swagUrlMaker("co.uk")
      case Some("IT") => swagUrlMaker("it")
      case Some("ES") => swagUrlMaker("es")
      case Some("NL") => swagUrlMaker("nl")
      case Some("PL") => swagUrlMaker("pl")
      case Some("BE") => swagUrlMaker("be")
      case Some("DK") => swagUrlMaker("dk")
      case Some("AU") => swagUrlMaker("com.au")
      case Some("IE") => swagUrlMaker("ie")
      case Some("NO") => swagUrlMaker("no")
      case Some("CH") => swagUrlMaker("ch")
      case Some("FI") => swagUrlMaker("fi")
      case Some("SE") => swagUrlMaker("se")
      case Some("AT") => swagUrlMaker("at")
      case _ => swagUrlMaker("net") // EU store as default

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
