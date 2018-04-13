package lila.streamer

import play.api.libs.json._

import lila.user.User

trait Stream {
  def serviceName: String
  val status: String
  val streamer: Streamer
  def is(s: Streamer): Boolean = streamer.id == s.id
  def is(userId: User.ID): Boolean = streamer.userId == userId
  def twitch = serviceName == "twitch"
  def youTube = serviceName == "youTube"
}

object Stream {

  case class Keyword(value: String) extends AnyRef with StringValue {
    def toLowerCase = value.toLowerCase
  }

  object Twitch {
    case class Channel(name: String, status: Option[String])
    case class TwitchStream(channel: Channel, stream_type: String) {
      def isLive = stream_type == "live"
    }
    case class Result(streams: Option[List[TwitchStream]]) {
      def liveStreams = (~streams).filter(_.isLive)
      def streams(keyword: Keyword, streamers: List[Streamer]): List[Stream] = liveStreams.map(_.channel).collect {
        case Channel(name, Some(status)) if status.toLowerCase contains keyword.toLowerCase =>
          streamers.find(s => s.twitch.exists(_.userId.toLowerCase == name.toLowerCase)) map { Stream(name, status, _) }
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
