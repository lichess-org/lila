package lila.user

import io.lemonlabs.uri.Url
import scala.util.Try

object Links {

  def make(text: String): List[Link] = text.linesIterator.to(List).map(_.trim).flatMap(toLink)

  private val UrlRegex = """^(?:https?://)?+([^/]+)""".r.unanchored

  private def toLink(line: String): Option[Link] =
    line match {
      case UrlRegex(domain) =>
        Link.Site.allKnown find (_ matches domain) orElse
          Try(Url.parse(domain).toStringPunycode).toOption.map(Link.Site.Other) map { site =>
            Link(
              site = site,
              url = if (line startsWith "http") line else s"https://$line"
            )
          }
      case _ => none
    }
}

case class Link(site: Link.Site, url: String)

object Link {

  sealed abstract class Site(val name: String, val domains: List[String]) {

    def matches(domain: String) =
      domains.exists { d =>
        domain == d || domain.endsWith(s".$d")
      }
  }

  object Site {
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
  }
}
