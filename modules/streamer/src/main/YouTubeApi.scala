package lila.streamer

import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import reactivemongo.api.bson.*

import lila.core.config.NetConfig
import lila.db.dsl.{ *, given }

import Stream.YouTube

final private class YouTubeApi(
    ws: StandaloneWSClient,
    coll: lila.db.dsl.Coll,
    keyword: Stream.Keyword,
    cfg: StreamerConfig,
    net: NetConfig
)(using Executor, akka.stream.Materializer):

  private var lastResults: List[YouTube.Stream] = Nil

  private case class Tuber(streamer: Streamer, youTube: Streamer.YouTube)

  def fetchStreams(streamers: List[Streamer]): Fu[List[YouTube.Stream]] =
    val maxResults = 50
    val tubers = streamers.flatMap { s => s.youTube.map(Tuber(s, _)) }
    val idPages = tubers
      .flatMap(tb => Seq(tb.youTube.pubsubVideoId, tb.youTube.liveVideoId).flatten)
      .distinct
      .grouped(maxResults)
    cfg.googleApiKey.value.nonEmpty
      .so:
        idPages.toList.sequentially: idPage =>
          ws.url("https://youtube.googleapis.com/youtube/v3/videos")
            .withQueryStringParameters(
              "part" -> "snippet",
              "id" -> idPage.mkString(","),
              "maxResults" -> s"$maxResults",
              "key" -> cfg.googleApiKey.value
            )
            .get()
            .map: rsp =>
              rsp.body[JsValue].validate[YouTube.Result] match
                case JsSuccess(data, _) =>
                  data.streams(keyword, tubers.map(_.streamer))
                case JsError(err) =>
                  logger.warn(s"youtube ${rsp.status} $err ${rsp.body[String].take(200)}")
                  Nil
      .map(_.flatten)
      .addEffect: streams =>
        if streams != lastResults then
          val newStreams = streams.filterNot(s => lastResults.exists(_.videoId == s.videoId))
          val goneStreams = lastResults.filterNot(s => streams.exists(_.videoId == s.videoId))
          if newStreams.nonEmpty then
            logger.info(s"fetchStreams NEW ${newStreams.map(_.channelId).mkString(" ")}")
          if goneStreams.nonEmpty then
            logger.info(s"fetchStreams GONE ${goneStreams.map(_.channelId).mkString(" ")}")
          syncDb(tubers, streams)
          lastResults = streams

  // youtube does not provide a low quota API to check for videos on a known channel id
  // and they don't provide the rss feed to non-browsers, so we're left to scrape the html.
  def forceCheckWithHtmlScraping(tuber: Streamer.YouTube) =
    ws.url(s"https://www.youtube.com/channel/${tuber.channelId}")
      .get()
      .map: rsp =>
        raw""""videoId":"(\S{11})"""".r
          .findFirstMatchIn(rsp.body)
          .foreach(m => onVideo(tuber.channelId, m.group(1)))

  def onVideoXml(xml: scala.xml.NodeSeq): Funit =
    val channel = (xml \ "entry" \ "channelId").text
    val video = (xml \ "entry" \ "videoId").text
    if channel.nonEmpty && video.nonEmpty
    then onVideo(channel, video)
    else
      val deleted = (xml \ "deleted-entry" \@ "ref")
      if deleted.nonEmpty
      then logger.debug(s"onYouTubeVideo deleted-entry $deleted")
      funit

  private def onVideo(channelId: String, videoId: String): Funit =
    import BsonHandlers.given
    coll
      .find($doc("youTube.channelId" -> channelId, "approval.granted" -> true))
      .sort($sort.desc("seenAt"))
      .cursor[Streamer]()
      .uno
      .flatMap:
        case Some(s) =>
          isLiveStream(videoId).map: isLive =>
            // this is the only notification we'll get, so don't filter offline users here.
            if isLive then
              logger.info(s"YouTube: LIVE ${s.id} vid:$videoId ch:$channelId")
              coll.update.one($doc("_id" -> s.id), $set("youTube.pubsubVideoId" -> videoId))
            else logger.debug(s"YouTube: IGNORED ${s.id} vid:$videoId ch:$channelId")
        case None =>
          fuccess:
            logger.info(s"YouTube: UNAPPROVED vid:$videoId ch:$channelId")

  private def isLiveStream(videoId: String): Fu[Boolean] =
    cfg.googleApiKey.value.nonEmpty.so(
      ws
        .url("https://youtube.googleapis.com/youtube/v3/videos")
        .withQueryStringParameters(
          "part" -> "snippet",
          "id" -> videoId,
          "key" -> cfg.googleApiKey.value
        )
        .get()
        .map { rsp =>
          rsp.body[JsValue].validate[YouTube.Result] match
            case JsSuccess(data, _) =>
              data.items.headOption.fold(false): item =>
                item.snippet.liveBroadcastContent == "live" && item.snippet.title.value.toLowerCase
                  .contains(keyword.toLowerCase)
            case JsError(err) =>
              logger.warn(s"YouTube ERROR: ${rsp.status} $err ${rsp.body[String].take(200)}")
              false
        }
    )

  def channelSubscribe(channelId: String, subscribe: Boolean): Funit = ws
    .url("https://pubsubhubbub.appspot.com/subscribe")
    .addHttpHeaders("content-type" -> "application/x-www-form-urlencoded")
    .post(
      asFormBody(
        "hub.callback" -> s"https://${net.domain}/api/x/streamer/youtube-pubsub",
        "hub.topic" -> s"https://www.youtube.com/xml/feeds/videos.xml?channel_id=$channelId",
        "hub.verify" -> "async",
        "hub.mode" -> (if subscribe then "subscribe" else "unsubscribe"),
        "hub.lease_seconds" -> s"${3600 * 24 * 10}" // 10 days seems to be the max
      )
    )
    .flatMap:
      case res if res.status / 100 == 2 =>
        logger.info(s"WebSub: REQUESTED ${if subscribe then "subscribe" else "unsubscribe"} on $channelId")
        funit
      case res =>
        logger.info(
          s"WebSub: FAILED ${if subscribe then "subscribe" else "unsubscribe"} on $channelId ${res.status}"
        )
        fufail(s"YouTubeApi.channelSubscribe $channelId failed ${res.status} ${res.body[String].take(200)}")

  private def asFormBody(params: (String, String)*): String =
    params.map((key, value) => s"$key=${java.net.URLEncoder.encode(value, "UTF-8")}").mkString("&")

  private def syncDb(tubers: List[Tuber], results: List[YouTube.Stream]): Funit =
    val bulk = coll.update(ordered = false)
    tubers
      .parallel: tuber =>
        val liveVid = results.find(_.channelId == tuber.youTube.channelId)
        bulk.element(
          q = $id(tuber.streamer.id),
          u = $doc(
            liveVid match
              case Some(v) => $set("youTube.liveVideoId" -> v.videoId) ++ $unset("youTube.pubsubVideoId")
              case None => $unset("youTube.liveVideoId", "youTube.pubsubVideoId")
          )
        )
      .map(bulk.many(_))

  private[streamer] def subscribeAll: Funit = cfg.googleApiKey.value.nonEmpty.so:
    import akka.stream.scaladsl.*
    import reactivemongo.akkastream.cursorProducer
    coll
      .find(
        $doc("youTube.channelId".$exists(true), "approval.granted" -> true),
        $doc("youTube.channelId" -> true).some
      )
      .cursor[Bdoc]()
      .documentSource()
      .mapConcat(_.getAsOpt[Bdoc]("youTube").flatMap(_.string("channelId")).toList)
      .mapAsyncUnordered(1) { channelSubscribe(_, true) }
      .toMat(lila.common.LilaStream.sinkCount)(Keep.right)
      .run()
      .map(nb => logger.info(s"YouTubeApi.subscribeAll: done $nb"))
