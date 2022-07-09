package lila.user

import io.mola.galimatias.URL
import scala.util.Try

object Links {

  def make(text: String): List[Link] =
    text.linesIterator
      .to(List)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map { line => if (line.contains("://")) line else s"https://$line" }
      .flatMap(toLink)

  private def toLink(line: String): Option[Link] =
    for {
      url <- Try(URL.parse(line)).toOption
      if url.scheme == "http" || url.scheme == "https"
      host <- Option(url.host).map(_.toHostString)
    } yield Link.Site.allKnown.find(_ matches host).map(site => Link(site, url.toString)) | Link(
      Link.Site.Other(host),
      url.toString
    )
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
