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

  sealed abstract class Site(val name: String, val domains: List[String]):

    def matches(domain: String) =
      domains.exists { d =>
        domain == d || domain.endsWith(s".$d")
      }

  object Site:
    case object Mastodon
        extends Site(
          "Mastodon",
          "mstdn.social fosstodon.org gensokyo.social ravenation.club mastodon.art mastodon.lol mastodon.green mas.to mindly.social mastodon.world masthead.social techhub.social"
            .split(' ')
            .toList
        )
    case object Twitter              extends Site("Twitter", List("twitter.com"))
    case object Facebook             extends Site("Facebook", List("facebook.com"))
    case object Instagram            extends Site("Instagram", List("instagram.com"))
    case object YouTube              extends Site("YouTube", List("youtube.com"))
    case object Twitch               extends Site("Twitch", List("twitch.tv"))
    case object GitHub               extends Site("GitHub", List("github.com"))
    case object VKontakte            extends Site("VKontakte", List("vk.com"))
    case object ChessCom             extends Site("Chess.com", List("chess.com"))
    case object Chess24              extends Site("Chess24", List("chess24.com"))
    case object ChessTempo           extends Site("ChessTempo", List("chesstempo.com"))
    case class Other(domain: String) extends Site(domain, List(domain))

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
      ChessTempo
    )

    val mastodonRegex = """@([\w-\+]+)@([\w-]+\.\w{2,})""".r
