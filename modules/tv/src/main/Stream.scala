package lila.tv

import com.roundeights.hasher.Implicits._
import play.api.libs.json._

case class StreamOnAir(
    service: String,
    name: String,
    url: String,
    streamId: String) {

  val id = url.md5.hex take 8
}

case class StreamsOnAir(streams: List[StreamOnAir])

object Twitch {
  case class Channel(url: String, status: String, name: String)
  case class Stream(channel: Channel)
  case class Result(streams: List[Stream]) {
    def streamsOnAir = streams map (_.channel) map { c =>
      StreamOnAir("twitch", c.status, c.url, c.name)
    }
  }
  object Reads {
    implicit val twitchChannelReads = Json.reads[Channel]
    implicit val twitchStreamReads = Json.reads[Stream]
    implicit val twitchResultReads: Reads[Result] = Json.reads[Result]
  }
}

object Ustream {
  case class Channel(url: String, title: String, id: String)
  case class Result(results: Option[List[Channel]]) {
    def streamsOnAir = ~results map { c =>
      StreamOnAir("ustream", c.title, c.url, c.id)
    }
  }
  object Reads {
    implicit val ustreamChannelReads = Json.reads[Channel]
    implicit val ustreamResultReads: Reads[Result] = Json.reads[Result]
  }
}
