package lila.tv

import play.api.libs.json._

case class StreamOnAir(
  name: String,
  url: String,
  author: String)

case class StreamsOnAir(streams: List[StreamOnAir])

object Twitch {
  case class Channel(url: String, status: String, display_name: String)
  case class Stream(channel: Channel)
  case class Result(streams: List[Stream])
  object Reads {
    implicit val twitchChannelReads = Json.reads[Channel]
    implicit val twitchStreamReads = Json.reads[Stream]
    implicit val twitchResultReads: Reads[Result] = Json.reads[Result]
  }
}
