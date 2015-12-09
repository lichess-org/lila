package lila.tv

import com.roundeights.hasher.Implicits._
import play.api.libs.json._
import StreamerList.Streamer

case class StreamOnAir(
    streamer: Streamer,
    name: String,
    url: String,
    streamId: String) {

  def id = streamer.id

  def highlight = streamer.streamerName != "ornicar2"
}

case class StreamsOnAir(streams: List[StreamOnAir])

object Twitch {
  case class Channel(url: Option[String], status: Option[String], name: String, display_name: String)
  case class Stream(channel: Channel)
  case class Result(streams: List[Stream]) {
    def streamsOnAir(streamers: List[Streamer]) =
      streams map (_.channel) flatMap { c =>
        (c.url, c.status, StreamerList.findTwitch(streamers)(c.display_name)) match {
          case (Some(url), Some(status), Some(streamer)) => Some(StreamOnAir(
            name = status,
            streamer = streamer,
            url = url,
            streamId = c.name
          ))
          case _ => None
        }
      }
  }
  object Reads {
    implicit val twitchChannelReads = Json.reads[Channel]
    implicit val twitchStreamReads = Json.reads[Stream]
    implicit val twitchResultReads: Reads[Result] = Json.reads[Result]
  }
}

object Hitbox {
  case class Channel(channel_link: String)
  case class Stream(channel: Channel, media_name: String, media_user_name: String, media_status: String, media_is_live: String)
  case class Result(livestream: List[Stream]) {
    def streamsOnAir(streamers: List[Streamer]) = livestream.flatMap { s =>
      for {
        streamer <- StreamerList.findTwitch(streamers)(s.media_user_name)
        if s.media_is_live == "1"
      } yield StreamOnAir(
        streamer = streamer,
        name = s.media_status,
        url = s.channel.channel_link,
        streamId = s.media_name)
    }
  }
  object Reads {
    implicit val hitboxChannelReads = Json.reads[Channel]
    implicit val hitboxStreamReads = Json.reads[Stream]
    implicit val hitboxResultReads: Reads[Result] = Json.reads[Result]
  }
}

// object Ustream {
//   case class Channel(url: String, title: String, id: String)
//   case class Result(results: Option[List[Channel]]) {
//     def streamsOnAir = ~results map { c =>
//       StreamOnAir("ustream", c.title.replace("(lichess.org)", ""), c.url, c.id)
//     }
//   }
//   object Reads {
//     implicit val ustreamChannelReads = Json.reads[Channel]
//     implicit val ustreamResultReads: Reads[Result] = Json.reads[Result]
//   }
// }
