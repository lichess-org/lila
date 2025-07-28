package lila.streamer

import play.api.i18n.Lang
import play.api.libs.json.*

import lila.common.Json.given
import lila.common.String.html.unescapeHtml
import lila.common.String.removeMultibyteSymbols
import lila.core.config.NetDomain
import lila.core.i18n.toLanguage

trait Stream:
  def serviceName: String
  val status: Html
  val streamer: Streamer
  val lang: Lang
  def urls: Stream.Urls

  def is[U: UserIdOf](u: U): Boolean = streamer.is(u)
  def twitch = serviceName == "twitch"
  def youTube = serviceName == "youTube"
  def language = toLanguage(lang)

  lazy val cleanStatus = status.map(s => removeMultibyteSymbols(s).trim)

object Stream:

  case class Keyword(value: String) extends AnyRef with StringValue:
    def toLowerCase = value.toLowerCase

  case class Urls(embed: NetDomain => String, redirect: String):
    def toPair(domain: NetDomain) = (embed(domain), redirect)

  object Twitch:
    case class TwitchStream(user_name: String, title: Html, `type`: String, language: String):
      def name = user_name
      def isLive = `type` == "live"
    case class Pagination(cursor: Option[String])
    case class Result(data: Option[List[TwitchStream]], pagination: Option[Pagination]):
      def liveStreams = (~data).filter(_.isLive)
    case class Stream(userId: String, status: Html, streamer: Streamer, lang: Lang)
        extends lila.streamer.Stream:
      def serviceName = "twitch"
      def urls = Urls(
        embed = parent => s"https://player.twitch.tv/?channel=${userId}&parent=${parent}",
        redirect = s"https://www.twitch.tv/${userId}"
      )
    private given Reads[TwitchStream] = Json.reads
    private given Reads[Pagination] = Json.reads
    given Reads[Result] = Json.reads

  object YouTube:
    case class Snippet(
        channelId: String,
        title: Html,
        liveBroadcastContent: String,
        defaultAudioLanguage: Option[String]
    )
    case class Item(id: String, snippet: Snippet)
    case class Result(items: List[Item]):
      def streams(keyword: Keyword, streamers: List[Streamer]): List[Stream] =
        items
          .withFilter: item =>
            item.snippet.liveBroadcastContent == "live" &&
              item.snippet.title.value.toLowerCase.contains(keyword.toLowerCase)
          .flatMap: item =>
            streamers
              .find(s => s.youTube.exists(_.channelId == item.snippet.channelId))
              .map:
                Stream(
                  item.snippet.channelId,
                  unescapeHtml(item.snippet.title),
                  item.id,
                  _,
                  item.snippet.defaultAudioLanguage.flatMap(Lang.get) | lila.core.i18n.defaultLang
                )
    case class Stream(
        channelId: String,
        status: Html,
        videoId: String,
        streamer: Streamer,
        lang: Lang
    ) extends lila.streamer.Stream:
      def serviceName = "youTube"
      def urls = Urls(
        embed = _ => s"https://www.youtube.com/embed/${videoId}?autoplay=1&disablekb=1&color=white",
        redirect = s"https://www.youtube.com/watch?v=${videoId}"
      )

    private given Reads[Snippet] = Json.reads
    private given Reads[Item] = Json.reads
    given Reads[Result] = Json.reads

  def toJson(picfit: lila.memo.PicfitUrl, stream: Stream) = Json.obj(
    "stream" -> Json.obj(
      "service" -> stream.serviceName,
      "status" -> stream.status,
      "lang" -> stream.lang
    ),
    "streamer" -> Json
      .obj("name" -> stream.streamer.name.value)
      .add("headline" -> stream.streamer.headline)
      .add("description" -> stream.streamer.description)
      .add("twitch" -> stream.streamer.twitch.map(_.fullUrl))
      .add("youTube" -> stream.streamer.youTube.map(_.fullUrl))
      .add("image" -> stream.streamer.picture.map { pic =>
        picfit.thumbnail(pic, Streamer.imageSize, Streamer.imageSize)
      })
  )
