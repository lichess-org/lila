package lila.tv

import play.api.libs.json._

case class StreamOnAir(
  name: String,
  url: String)

case class StreamsOnAir(streams: List[StreamOnAir])

object Twitch {
  case class Channel(url: String, status: String)
  case class Stream(channel: Channel)
  case class Result(streams: List[Stream]) {
    def streamsOnAir = streams map (_.channel) map { c =>
      StreamOnAir(c.status, c.url)
    }
  }
  object Reads {
    implicit val twitchChannelReads = Json.reads[Channel]
    implicit val twitchStreamReads = Json.reads[Stream]
    implicit val twitchResultReads: Reads[Result] = Json.reads[Result]
  }
}

object Ustream {
  case class Channel(url: String, title: String)
  case class Result(results: List[Channel]) {
    def streamsOnAir = results map { c =>
      StreamOnAir(c.title, c.url)
    }
  }
  object Reads {
    implicit val ustreamChannelReads = Json.reads[Channel]
    implicit val ustreamResultReads: Reads[Result] = Json.reads[Result]
  }
}
