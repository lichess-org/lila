package lila.streamer

import org.joda.time.DateTime
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration.*

import lila.common.{ given, * }
import lila.common.config.NetConfig
import lila.common.Json.given
import lila.db.dsl.{ *, given }
import Stream.YouTube

final private class YouTubeApi(
    ws: StandaloneWSClient,
    coll: lila.db.dsl.Coll,
    keyword: Stream.Keyword,
    cfg: StreamerConfig,
    net: NetConfig
)(using ec: scala.concurrent.ExecutionContext):

  private var lastResults: List[YouTube.Stream] = List()

  def fetchStreams(streamers: List[Streamer]): Fu[List[YouTube.Stream]] =
    val maxResults = 50
    val tubers     = streamers.filter(_.youTube.isDefined)
    val idPages = tubers
      .flatMap(x => Seq(x.youTube.get.pubsubVideoId, x.youTube.get.liveVideoId).flatten)
      .distinct
      .grouped(maxResults)

    cfg.googleApiKey.value.nonEmpty ?? scala.concurrent.Future
      .sequence {
        idPages.map { idPage =>
          ws.url("https://youtube.googleapis.com/youtube/v3/videos")
            .withQueryStringParameters(
              "part"       -> "snippet",
              "id"         -> idPage.mkString(","),
              "maxResults" -> s"$maxResults",
              "key"        -> cfg.googleApiKey.value
            )
            .get()
            .map { rsp =>
              rsp.body[JsValue].validate[YouTube.Result] match
                case JsSuccess(data, _) =>
                  data.streams(keyword, tubers)
                case JsError(err) =>
                  logger.warn(s"youtube ${rsp.status} $err ${rsp.body[String].take(200)}")
                  Nil
            }
        }.toList
      }
      .map(_.flatten)
      .addEffect { streams =>
        if streams != lastResults then
          syncDb(tubers, streams)
          lastResults = streams
      }

  def onVideo(channelId: String, videoId: String): Funit = coll.update
    .one(
      $doc("youTube.channelId"     -> channelId, "approval.granted" -> true),
      $set("youTube.pubsubVideoId" -> videoId),
      multi = true // ?
    )
    .void

  def channelSubscribe(channelId: String, subscribe: Boolean): Funit = ws
    .url("https://pubsubhubbub.appspot.com/subscribe")
    .addHttpHeaders("content-type" -> "application/x-www-form-urlencoded")
    .post(
      asFormBody(
        "hub.callback"      -> s"https://${net.domain}/api/x/streamer/youtube-pubsub",
        "hub.topic"         -> s"https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelId",
        "hub.verify"        -> "async",
        "hub.mode"          -> (if subscribe then "subscribe" else "unsubscribe"),
        "hub.lease_seconds" -> s"${3600 * 24 * 10}" // 10 days seems to be the max
      )
    )
    .void

  private def asFormBody(params: (String, String)*): String =
    s"${params.map(x => s"${x._1}=${java.net.URLEncoder.encode(x._2, "UTF-8")}").toList.mkString("&")}"

  private def syncDb(tubers: List[Streamer], results: List[YouTube.Stream]): Funit =
    val bulk = coll.update(ordered = false)
    tubers
      .map { tuber =>
        val liveVid = results.find(_.channelId == tuber.youTube.get.channelId)
        bulk.element(
          q = $id(tuber.id),
          u = $doc(
            liveVid match
              case Some(v) => $set("youTube.liveVideoId" -> v.videoId) ++ $unset("youTube.pubsubVideoId")
              case None    => $unset("youTube.liveVideoId", "youTube.pubsubVideoId")
          )
        )
      }
      .sequenceFu
      .map(bulk many _)

  private[streamer] def subscribeAll: Funit = coll
    .find(
      $doc("youTube.channelId" $exists true),
      $doc("youTube.channelId" -> true).some
    )
    .cursor[Bdoc]()
    .list(10000)
    .map { docs =>
      for
        doc       <- docs
        ytDoc     <- doc.getAsOpt[Bdoc]("youTube")
        channelId <- ytDoc string "channelId"
      yield channelSubscribe(channelId, true)
    }
