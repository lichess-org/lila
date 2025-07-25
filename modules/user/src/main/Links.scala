package lila.user

object Links:

  import Link.*

  def make(text: String): List[Link] =
    text.linesIterator.toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap:
        case Site.mastodonRegex(user, server) => Link(Site.Mastodon, s"https://$server/@$user").some
        case line => toLink(if line.contains("://") then line else s"https://$line")

  private def toLink(line: String): Option[Link] =
    for
      url <- lila.common.url.parse(line).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toHostString)
    yield Site.allKnown.find(_.matches(host)).map(site => Link(site, url.toString)) | Link(
      Site.Other(host),
      url.toString
    )

case class Link(site: Link.Site, url: String)

object Link:

  enum Site(val name: String, val domains: List[String]):

    def matches(domain: String) =
      domains.exists: d =>
        domain == d || domain.endsWith(s".$d")

    case Mastodon
        extends Site(
          "Mastodon",
          "mastodon.social mastodon.online mstdn.social masto.ai fosstodon.org gensokyo.social ravenation.club mastodon.art mastodon.green mas.to mindly.social mastodon.world techhub.social im-in.space mastodon.cloud"
            .split(' ')
            .toList
        )
    case Bluesky extends Site("Bluesky", List("bsky.app"))
    case Twitter extends Site("x", List("twitter.com", "x.com"))
    case Facebook extends Site("Facebook", List("facebook.com"))
    case Instagram extends Site("Instagram", List("instagram.com"))
    case YouTube extends Site("YouTube", List("youtube.com"))
    case Twitch extends Site("Twitch", List("twitch.tv"))
    case GitHub extends Site("GitHub", List("github.com"))
    case VKontakte extends Site("VKontakte", List("vk.com"))
    case ChessCom extends Site("Chess.com", List("chess.com"))
    case ChessMonitor extends Site("ChessMonitor", List("chessmonitor.com"))
    case ChessTempo extends Site("ChessTempo", List("chesstempo.com"))
    case Other(domain: String) extends Site(domain, List(domain))

  object Site:
    val allKnown: List[Site] = List(
      Mastodon,
      Bluesky,
      Twitter,
      Facebook,
      Instagram,
      YouTube,
      Twitch,
      GitHub,
      VKontakte,
      ChessCom,
      ChessMonitor,
      ChessTempo
    )

    val mastodonRegex = """@([\w-\+]+)@([\w-]+\.\w{2,})""".r
