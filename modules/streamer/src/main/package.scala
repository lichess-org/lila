package lila.streamer

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

type Platform = "twitch" | "youtube"
def platform(str: String): Option[Platform] = str.toLowerCase match
  case "twitch" => Some("twitch")
  case "youtube" => Some("youtube")
  case _ => None

private val logger = lila.log("streamer")

private val streamerPageActivationRoute =
  routes.Cms.lonePage(lila.core.id.CmsPageKey("streamer-page-activation"))
