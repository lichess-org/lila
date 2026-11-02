package lila.streamer

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

type Platform = "twitch" | "youtube"
def platform(str: String): Option[Platform] = str.toLowerCase match
  case "twitch" => Option("twitch")
  case "youtube" => Option("youtube")
  case _ => None

private val streamerPageActivationRoute =
  routes.Cms.lonePage(lila.core.id.CmsPageKey("streamer-page-activation"))
