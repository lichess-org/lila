package lila.streamer

import play.api.libs.json._

trait Stream {
  def serviceName: String
  val status: String
  val streamer: Streamer
  def is(s: Streamer): Boolean = streamer.id == s.id
  def twitch = serviceName == "twitch"
  def youTube = serviceName == "youTube"
}

object Stream {

  case class Keyword(value: String) extends AnyRef with StringValue {
    def toLowerCase = value.toLowerCase
  }

  case class LiveStreams(streams: List[Stream]) {

    def has(streamer: Streamer) = streams.exists(_ is streamer)

    def get(streamer: Streamer) = streams.find(_ is streamer)

    def autoFeatured = LiveStreams {
      streams.filter(_.streamer.approval.autoFeatured)
    }
  }

  object Twitch {
    case class Channel(name: String, status: Option[String])
    case class TwitchStream(channel: Channel)
    case class Result(streams: Option[List[TwitchStream]]) {
      def streams(keyword: Keyword, streamers: List[Streamer]): List[Stream] = (~streams).map(_.channel).collect {
        case Channel(name, Some(status)) if status.toLowerCase contains keyword.toLowerCase =>
          streamers.find(s => s.twitch.exists(_.userId == name)) map { Stream(name, status, _) }
      }.flatten
    }
    case class Stream(userId: String, status: String, streamer: Streamer) extends lila.streamer.Stream {
      def serviceName = "twitch"
    }
    object Reads {
      private implicit val twitchChannelReads = Json.reads[Channel]
      private implicit val twitchStreamReads = Json.reads[TwitchStream]
      implicit val twitchResultReads = Json.reads[Result]
    }
  }

  object YouTube {
    case class Snippet(channelId: String, title: String, liveBroadcastContent: String)
    case class Id(videoId: String)
    case class Item(id: Id, snippet: Snippet)
    case class Result(items: List[Item]) {
      def streams(keyword: Keyword, streamers: List[Streamer]): List[Stream] =
        items.filter { item =>
          item.snippet.liveBroadcastContent == "live" &&
            item.snippet.title.toLowerCase.contains(keyword.toLowerCase)
        }.flatMap { item =>
          streamers.find(s => s.youTube.exists(_.channelId == item.snippet.channelId)) map {
            Stream(item.snippet.channelId, item.snippet.title, item.id.videoId, _)
          }
        }
    }
    case class Stream(channelId: String, status: String, videoId: String, streamer: Streamer) extends lila.streamer.Stream {
      def serviceName = "youTube"
    }

    object Reads {
      private implicit val youtubeSnippetReads = Json.reads[Snippet]
      private implicit val youtubeIdReads = Json.reads[Id]
      private implicit val youtubeItemReads = Json.reads[Item]
      implicit val youtubeResultReads = Json.reads[Result]
    }
  }
}
