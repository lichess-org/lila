package lila.streamer

import play.api.libs.json.*

import lila.common.String.html.unescapeHtml
import lila.common.String.removeMultibyteSymbols
import lila.common.Json.given

trait Stream:
  def serviceName: String
  val status: Html
  val streamer: Streamer
  val language: String

  def is(s: Streamer): Boolean    = streamer is s
  def is(userId: UserId): Boolean = streamer is userId
  def twitch                      = serviceName == "twitch"
  def youTube                     = serviceName == "youTube"

  lazy val cleanStatus = status.map(s => removeMultibyteSymbols(s).trim)

  lazy val lang: String = (language.length == 2) so language.toLowerCase

object Stream:

  case class Keyword(value: String) extends AnyRef with StringValue:
    def toLowerCase = value.toLowerCase

  object Twitch:
    case class TwitchStream(user_name: String, title: Html, `type`: String, language: String):
      def name   = user_name
      def isLive = `type` == "live"
    case class Pagination(cursor: Option[String])
    case class Result(data: Option[List[TwitchStream]], pagination: Option[Pagination]):
      def liveStreams = (~data).filter(_.isLive)
    case class Stream(userId: String, status: Html, streamer: Streamer, language: String)
        extends lila.streamer.Stream:
      def serviceName = "twitch"
    private given Reads[TwitchStream] = Json.reads
    private given Reads[Pagination]   = Json.reads
    given Reads[Result]               = Json.reads

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
          .withFilter { item =>
            item.snippet.liveBroadcastContent == "live" &&
            item.snippet.title.value.toLowerCase.contains(keyword.toLowerCase)
          }
          .flatMap { item =>
            streamers.find(s => s.youTube.exists(_.channelId == item.snippet.channelId)) map {
              Stream(
                item.snippet.channelId,
                unescapeHtml(item.snippet.title),
                item.id,
                _,
                ~item.snippet.defaultAudioLanguage
              )
            }
          }
    case class Stream(
        channelId: String,
        status: Html,
        videoId: String,
        streamer: Streamer,
        language: String
    ) extends lila.streamer.Stream:
      def serviceName = "youTube"

    private given Reads[Snippet] = Json.reads
    private given Reads[Item]    = Json.reads
    given Reads[Result]          = Json.reads

  def toJson(picfit: lila.memo.PicfitUrl, stream: Stream) = Json.obj(
    "stream" -> Json.obj(
      "service" -> stream.serviceName,
      "status"  -> stream.status,
      "lang"    -> stream.lang
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
