package lila.streamer

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.Play.current
import play.twirl.api.Html
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.User

private final class Streaming(
    renderer: ActorSelection,
    api: StreamerApi,
    isOnline: User.ID => Boolean,
    keyword: Stream.Keyword,
    googleApiKey: String,
    twitchClientId: String
) extends Actor {

  import Stream._
  import Twitch.Reads._
  import YouTube.Reads._
  import BsonHandlers._

  private case object Tick

  private def scheduleTick =
    context.system.scheduler.scheduleOnce(15 seconds, self, Tick)

  private var liveStreams = LiveStreams(Nil)

  def receive = {

    case Streaming.Get => sender ! liveStreams

    case Tick => updateStreams addEffectAnyway scheduleTick
  }

  def updateStreams: Funit = for {
    streamers <- api.allListed.map {
      _.filter { streamer =>
        streamer.sorting.streaming || isOnline(streamer.userId)
      }
    }
    (twitchStreams, youTubeStreams) <- fetchTwitchStreams(streamers) zip fetchYouTubeStreams(streamers)
    now = DateTime.now
    _ <- finishGoneStreams(streamers, twitchStreams, youTubeStreams, now)
    _ <- updateCurrentStreams(streamers, twitchStreams, youTubeStreams, now)
  } yield publishStreams(streamers, LiveStreams(twitchStreams ::: youTubeStreams))

  def finishGoneStreams(streamers: List[Streamer], twitch: List[Twitch.Stream], youTube: List[YouTube.Stream], now: DateTime): Funit =
    streamers.map { s =>
      val onTwitch = twitch.exists(_ is s)
      val onYouTube = youTube.exists(_ is s)
      val goneFromTwitch = s.twitch.exists(_.live.now) && !onTwitch
      val goneFromYouTube = s.youTube.exists(_.live.now) && !onYouTube
      val noLongerStreaming = s.sorting.streaming && !onTwitch && !onYouTube
      (goneFromTwitch || goneFromYouTube || noLongerStreaming) ??
        api.withColl {
          _.update(
            $id(s.id),
            $doc("$set" -> {
              $doc("sorting.streaming" -> false) ++
                s.twitch.isDefined.??($doc("twitch.live.checkedAt" -> now)) ++
                s.youTube.isDefined.??($doc("youTube.live.checkedAt" -> now))
            })
          ).void
        }
    }.sequenceFu.void

  def updateCurrentStreams(streamers: List[Streamer], twitch: List[Twitch.Stream], youTube: List[YouTube.Stream], now: DateTime): Funit =
    (twitch ::: youTube).map { s =>
      api.withColl {
        _.update(
          $id(s.streamer.id),
          $set(
            "sorting.streaming" -> true,
            s"${s.serviceName}.live.liveAt" -> now,
            s"${s.serviceName}.live.status" -> s.status,
            s"${s.serviceName}.live.checkedAt" -> now
          )
        ).void
      }
    }.sequenceFu.void

  var lastPublishedHtml: Html = Html("")

  def publishStreams(streamers: List[Streamer], liveStreams: LiveStreams) = {
    import makeTimeout.short
    import akka.pattern.ask
    renderer ? liveStreams foreach {
      case html: play.twirl.api.Html =>
        if (html != lastPublishedHtml) {
          lastPublishedHtml = html
          context.system.lilaBus.publish(lila.hub.actorApi.StreamsOnAir(html.body), 'streams)
        }
    }
    streamers foreach { streamer =>
      streamer.twitch.foreach { t =>
        lila.mon.tv.stream.name(s"${t.userId}@twitch") {
          if (liveStreams.streams.exists(s => s.serviceName == "twitch" && s.is(streamer))) 1 else 0
        }
      }
      streamer.youTube.foreach { t =>
        lila.mon.tv.stream.name(s"${t.channelId}@youtube") {
          if (liveStreams.streams.exists(s => s.serviceName == "youTube" && s.is(streamer))) 1 else 0
        }
      }
    }
  }

  def fetchTwitchStreams(streamers: List[Streamer]): Fu[List[Twitch.Stream]] =
    WS.url("https://api.twitch.tv/kraken/streams")
      .withQueryString("channel" -> streamers.flatMap(_.twitch).map(_.userId).mkString(","))
      .withHeaders(
        "Accept" -> "application/vnd.twitchtv.v3+json",
        "Client-ID" -> twitchClientId
      )
      .get() map { res =>
        res.json.validate[Twitch.Result](twitchResultReads) match {
          case JsSuccess(data, _) => data.streams(keyword, streamers)
          case JsError(err) =>
            logger.warn(s"twitch ${res.status} $err ${~res.body.lines.toList.headOption}")
            Nil
        }
      }

  def fetchYouTubeStreams(streamers: List[Streamer]): Fu[List[YouTube.Stream]] = googleApiKey.nonEmpty ?? {
    WS.url("https://www.googleapis.com/youtube/v3/search").withQueryString(
      "part" -> "snippet",
      "type" -> "video",
      "eventType" -> "live",
      "q" -> keyword.value,
      "key" -> googleApiKey
    ).get() map { res =>
        res.json.validate[YouTube.Result](youtubeResultReads) match {
          case JsSuccess(data, _) => data.streams(keyword, streamers)
          case JsError(err) =>
            logger.warn(s"youtube ${res.status} $err ${~res.body.lines.toList.headOption}")
            Nil
        }
      }
  }
}

object Streaming {

  case object Get
}
