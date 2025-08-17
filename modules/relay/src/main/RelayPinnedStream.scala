package lila.relay

import io.mola.galimatias.URL
import scala.jdk.CollectionConverters.*
import lila.core.config.NetDomain

case class RelayPinnedStream(name: String, url: URL, text: Option[String]):

  import RelayPinnedStream.*

  def upstream: Option[RelayPinnedStream.Upstream] =
    parseYoutube.orElse(parseTwitch)

  def parseYoutube: Option[YouTube] =
    if List("www.youtube.com", "youtube.com", "youtu.be").contains(url.host.toString) then
      url.pathSegments.asScala.toList match
        case List("live", id) => Some(YouTube(id))
        case _ => Option(url.queryParameter("v")).map(YouTube.apply)
    else None

  // https://www.twitch.tv/tcec_chess_tv
  def parseTwitch: Option[Twitch] =
    url.host.toString
      .endsWith("twitch.tv")
      .so:
        url.pathSegments.asScala.toList match
          case List(id) => Twitch(id).some
          case _ => none

object RelayPinnedStream:
  case class Urls(embed: NetDomain => String, redirect: String):
    def toPair(domain: NetDomain) = (embed(domain), redirect)
  sealed trait Upstream:
    def urls: Urls
  case class YouTube(id: String) extends Upstream:
    def urls = Urls(
      _ => s"https://www.youtube.com/embed/${id}?disablekb=1&modestbranding=1&autoplay=1",
      s"https://www.youtube.com/watch?v=${id}"
    )
  case class Twitch(id: String) extends Upstream:
    def urls = Urls(
      parent => s"https://player.twitch.tv/?channel=${id}&parent=${parent}&autoplay=true",
      s"https://www.twitch.tv/${id}"
    )
