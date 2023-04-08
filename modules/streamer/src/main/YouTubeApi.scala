package lila.streamer

import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.StandaloneWSClient

import lila.common.{ given, * }
import lila.common.config.NetConfig
import lila.db.dsl.{ *, given }
import Stream.YouTube

final private class YouTubeApi(
    ws: StandaloneWSClient,
    coll: lila.db.dsl.Coll,
    keyword: Stream.Keyword,
    cfg: StreamerConfig,
    net: NetConfig
)(using Executor, akka.stream.Materializer):

  private var lastResults: List[YouTube.Stream] = List()

  private case class Tuber(streamer: Streamer, youTube: Streamer.YouTube)

  def fetchStreams(streamers: List[Streamer]): Fu[List[YouTube.Stream]] =
    val maxResults = 50
    val tubers     = streamers.flatMap { s => s.youTube.map(Tuber(s, _)) }
    val idPages = tubers
      .flatMap(tb => Seq(tb.youTube.pubsubVideoId, tb.youTube.liveVideoId).flatten)
      .distinct
      .grouped(maxResults)

    cfg.googleApiKey.value.nonEmpty ?? Future
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
                  data.streams(keyword, tubers.map(_.streamer))
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
    .flatMap {
      case res if res.status / 100 != 2 =>
        fufail(s"YouTubeApi.channelSubscribe status: ${res.status} ${res.body[String].take(200)}")
      case _ => funit
    }

  private def asFormBody(params: (String, String)*): String =
    params.map((key, value) => s"$key=${java.net.URLEncoder.encode(value, "UTF-8")}").mkString("&")

  private def syncDb(tubers: List[Tuber], results: List[YouTube.Stream]): Funit =
    val bulk = coll.update(ordered = false)
    tubers
      .map { tuber =>
        val liveVid = results.find(_.channelId == tuber.youTube.channelId)
        bulk.element(
          q = $id(tuber.streamer.id),
          u = $doc(
            liveVid match
              case Some(v) => $set("youTube.liveVideoId" -> v.videoId) ++ $unset("youTube.pubsubVideoId")
              case None    => $unset("youTube.liveVideoId", "youTube.pubsubVideoId")
          )
        )
      }
      .parallel
      .map(bulk many _)

  private[streamer] def subscribeAll: Funit = cfg.googleApiKey.value.nonEmpty ?? {
    import akka.stream.scaladsl.*
    import reactivemongo.akkastream.cursorProducer
    coll
      .find(
        $doc("youTube.channelId" $exists true, "approval.granted" -> true),
        $doc("youTube.channelId" -> true).some
      )
      .cursor[Bdoc]()
      .documentSource()
      .mapConcat(_.getAsOpt[Bdoc]("youTube").flatMap(_.string("channelId")).toList)
      .mapAsyncUnordered(1) { channelSubscribe(_, true) }
      .toMat(lila.common.LilaStream.sinkCount)(Keep.right)
      .run()
      .map(nb => logger.info(s"YouTubeApi.subscribeAll: done $nb"))
  }
