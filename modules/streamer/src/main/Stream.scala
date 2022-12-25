package lila.streamer

import play.api.libs.json.*
import org.joda.time.DateTime

import lila.user.User
import lila.common.String.html.unescapeHtml
import lila.common.String.removeMultibyteSymbols
import lila.common.Json.given

trait Stream:
  def serviceName: String
  val status: String
  val streamer: Streamer
  val language: String

  def is(s: Streamer): Boolean    = streamer.id == s.id
  def is(userId: UserId): Boolean = streamer.userId == userId
  def twitch                      = serviceName == "twitch"
  def youTube                     = serviceName == "youTube"

  lazy val cleanStatus = removeMultibyteSymbols(status).trim

  lazy val lang: String = (language.length == 2) ?? language.toLowerCase

object Stream:

  case class Keyword(value: String) extends AnyRef with StringValue:
    def toLowerCase = value.toLowerCase

  object Twitch:
    case class TwitchStream(user_name: String, title: String, `type`: String, language: String):
      def name   = user_name
      def isLive = `type` == "live"
    case class Pagination(cursor: Option[String])
    case class Result(data: Option[List[TwitchStream]], pagination: Option[Pagination]):
      def liveStreams = (~data).filter(_.isLive)
    case class Stream(userId: String, status: String, streamer: Streamer, language: String)
        extends lila.streamer.Stream:
      def serviceName = "twitch"
    private given Reads[TwitchStream] = Json.reads
    private given Reads[Pagination]   = Json.reads
    given Reads[Result]               = Json.reads

  object YouTube:
    case class Snippet(
        channelId: String,
        title: String,
        liveBroadcastContent: String,
        defaultAudioLanguage: Option[String]
    )
    case class Id(videoId: String)
    case class Item(id: Id, snippet: Snippet)
    case class Result(items: List[Item]):
      def streams(keyword: Keyword, streamers: List[Streamer]): List[Stream] =
        items
          .withFilter { item =>
            item.snippet.liveBroadcastContent == "live" &&
            item.snippet.title.toLowerCase.contains(keyword.toLowerCase)
          }
          .flatMap { item =>
            streamers.find(s => s.youTube.exists(_.channelId == item.snippet.channelId)) map {
              Stream(
                item.snippet.channelId,
                unescapeHtml(item.snippet.title),
                item.id.videoId,
                _,
                ~item.snippet.defaultAudioLanguage
              )
            }
          }
    case class Stream(
        channelId: String,
        status: String,
        videoId: String,
        streamer: Streamer,
        language: String
    ) extends lila.streamer.Stream:
      def serviceName = "youTube"

    private given Reads[Snippet] = Json.reads
    private given Reads[Id]      = Json.reads
    private given Reads[Item]    = Json.reads
    given Reads[Result]          = Json.reads

    case class StreamsFetched(list: List[YouTube.Stream], at: DateTime)

  def toJson(stream: Stream) = Json.obj(
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
  )

  private val LangRegex = """\[(\w\w)\]""".r.unanchored
