package lila.api

import play.api.libs.json.{ Json, JsArray }
import lila.common.config.NetConfig
import lila.common.Json.given

object StaticContent:

  val robotsTxt = """User-agent: *
Allow: /
Disallow: /game/export/
Disallow: /games/export/
Disallow: /api/
Disallow: /opening/config/
Allow: /game/export/gif/thumbnail/

User-agent: Twitterbot
Allow: /
"""

  def manifest(net: NetConfig) =
    Json.obj(
      "name"             -> net.domain,
      "short_name"       -> "Lichess",
      "start_url"        -> "/",
      "display"          -> "standalone",
      "background_color" -> "#161512",
      "theme_color"      -> "#161512",
      "description"      -> "The (really) free, no-ads, open source chess server.",
      "icons" -> List(32, 64, 128, 192, 256, 512, 1024).map: size =>
        Json.obj(
          "src"   -> s"//${net.assetDomain}/assets/logo/lichess-favicon-$size.png",
          "sizes" -> s"${size}x$size",
          "type"  -> "image/png"
        ),
      "related_applications" -> Json.arr(
        Json.obj(
          "platform" -> "play",
          "url"      -> "https://play.google.com/store/apps/details?id=org.lichess.mobileapp"
        ),
        Json.obj(
          "platform" -> "itunes",
          "url"      -> "https://itunes.apple.com/us/app/lichess-free-online-chess/id968371784"
        )
      )
    )

  lazy val variantsJson =
    JsArray(chess.variant.Variant.list.all.map { v =>
      Json.obj(
        "id"   -> v.id,
        "key"  -> v.key,
        "name" -> v.name
      )
    })
