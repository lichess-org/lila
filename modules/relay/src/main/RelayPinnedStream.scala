package lila.relay

import io.mola.galimatias.URL
import scala.jdk.CollectionConverters.*
import lila.core.config.NetDomain

case class RelayPinnedStream(name: String, url: URL):

  import RelayPinnedStream.*

  def upstream: Option[RelayPinnedStream.Upstream] =
    parseYoutube.orElse(parseTwitch)

  // https://www.youtube.com/live/Lg0askmGqvo
  // https://www.youtube.com/live/Lg0askmGqvo?si=KKOexnmA2xPcyStZ
  def parseYoutube: Option[YouTube] =
    url.host.toString
      .endsWith("youtube.com")
      .so:
        url.pathSegments.asScala.toList match
          case List("live", id) => YouTube(id).some
          case _                => none

  // https://www.twitch.tv/tcec_chess_tv
  def parseTwitch: Option[Twitch] =
    url.host.toString
      .endsWith("twitch.tv")
      .so:
        url.pathSegments.asScala.toList match
          case List(id) => Twitch(id).some
          case _        => none

object RelayPinnedStream:
  case class Urls(embed: String, redirect: String):
    def toPair = (embed, redirect)
  sealed trait Upstream:
    def urls(parent: NetDomain): Urls
  case class YouTube(id: String) extends Upstream:
    def urls(parent: NetDomain) = Urls(
      s"https://www.youtube.com/embed/${id}?disablekb=1&modestbranding=1",
      s"https://www.youtube.com/watch?v=${id}"
    )
  case class Twitch(id: String) extends Upstream:
    def urls(parent: NetDomain) = Urls(
      s"https://player.twitch.tv/?channel=${id}&parent=${parent}",
      s"https://www.twitch.tv/${id}"
    )
