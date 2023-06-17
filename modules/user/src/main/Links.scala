package lila.user

import io.mola.galimatias.URL
import scala.util.Try

object Links:

  import Link.*

  def make(text: String): List[Link] =
    text.linesIterator.toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap {
        case Site.mastodonRegex(user, server) => Link(Site.Mastodon, s"https://$server/@$user").some
        case line => toLink(if line.contains("://") then line else s"https://$line")
      }

  private def toLink(line: String): Option[Link] =
    for {
      url <- Try(URL.parse(line)).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toHostString)
    } yield Site.allKnown.find(_ matches host).map(site => Link(site, url.toString)) | Link(
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
          "mstdn.social fosstodon.org gensokyo.social ravenation.club mastodon.art mastodon.lol mastodon.green mas.to mindly.social mastodon.world masthead.social techhub.social"
            .split(' ')
            .toList
        )
    case Twitter               extends Site("Twitter", List("twitter.com"))
    case Facebook              extends Site("Facebook", List("facebook.com"))
    case Instagram             extends Site("Instagram", List("instagram.com"))
    case YouTube               extends Site("YouTube", List("youtube.com"))
    case Twitch                extends Site("Twitch", List("twitch.tv"))
    case GitHub                extends Site("GitHub", List("github.com"))
    case VKontakte             extends Site("VKontakte", List("vk.com"))
    case ChessCom              extends Site("Chess.com", List("chess.com"))
    case Chess24               extends Site("Chess24", List("chess24.com"))
    case ChessMonitor          extends Site("ChessMonitor", List("chessmonitor.com"))
    case ChessTempo            extends Site("ChessTempo", List("chesstempo.com"))
    case Other(domain: String) extends Site(domain, List(domain))

  object Site:
    val allKnown: List[Site] = List(
      Mastodon,
      Twitter,
      Facebook,
      Instagram,
      YouTube,
      Twitch,
      GitHub,
      VKontakte,
      ChessCom,
      Chess24,
      ChessMonitor,
      ChessTempo
    )

    val mastodonRegex = """@([\w-\+]+)@([\w-]+\.\w{2,})""".r
