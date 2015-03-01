package lila.tv

import com.roundeights.hasher.Implicits._
import play.api.libs.json._

case class StreamOnAir(
    service: String,
    name: String,
    streamer: String,
    streamerName: String,
    url: String,
    streamId: String) {

  val id = url.md5.hex take 8
}

case class StreamsOnAir(streams: List[StreamOnAir])

object Twitch {
  case class Channel(url: String, status: String, name: String, display_name: String)
  case class Stream(channel: Channel)
  case class Result(streams: List[Stream]) {
    def streamsOnAir = streams map (_.channel) map { c =>
      StreamOnAir(
        service = "twitch",
        name = c.status,
        streamer = c.name,
        streamerName = c.display_name,
        url = c.url,
        streamId = c.name
      )
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
    def streamsOnAir = livestream flatMap { s =>
      (s.media_is_live == "1") option StreamOnAir(
        service = "hitbox",
        name = s.media_status,
        streamer = s.media_user_name,
        streamerName = s.media_user_name,
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
