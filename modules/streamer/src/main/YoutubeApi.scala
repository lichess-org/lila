package lila.streamer

import play.api.i18n.Lang
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import lila.common.Json.given
import lila.common.String.html.unescapeHtml
import lila.common.autoconfig.ConfigName
import lila.core.config.{ NetConfig, Secret }

private class YoutubeConfig(
    @ConfigName("api_key") val apiKey: Secret,
    @ConfigName("client_id") val clientId: String,
    @ConfigName("client_secret") val clientSecret: Secret
)

final private class YoutubeApi(
    ws: StandaloneWSClient,
    repo: StreamerRepo,
    keyword: Stream.Keyword,
    cfg: YoutubeConfig,
    net: NetConfig
)(using Executor):

  private var lastResults: List[Youtube.YoutubeStream] = Nil
  private val endpoint = "https://youtube.googleapis.com/youtube/v3"

  def liveMatching(streamers: List[Streamer]): Fu[List[Youtube.YoutubeStream]] =
    val maxResults = 50
    val tubers = streamers.flatMap { s => s.youTube.map(Youtube.StreamerWithYoutube(s, _)) }
    val idPages = tubers
      .flatMap(tb => Seq(tb.youtube.pubsubVideoId, tb.youtube.liveVideoId).flatten)
      .distinct
      .grouped(maxResults)
    cfg.apiKey.value.nonEmpty
      .so:
        idPages.toList.sequentially: idPage =>
          ws.url(s"$endpoint/videos")
            .withQueryStringParameters(
              "part" -> "snippet",
              "id" -> idPage.mkString(","),
              "maxResults" -> s"$maxResults",
              "key" -> cfg.apiKey.value
            )
            .get()
            .map: rsp =>
              rsp.body[JsValue].validate[Youtube.Result] match
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
          repo.updateYoutubeChannels(tubers, streams)
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
        fufail(s"YouTubeApi.channelSubscribe $channelId failed ${lila.log.http(res.status, res.body)}")

  def authorizeUrl(redirectUri: String, state: String, forceVerify: Boolean): String =
    val params = Map(
      "client_id" -> cfg.clientId,
      "redirect_uri" -> redirectUri,
      "response_type" -> "code",
      "scope" -> "https://www.googleapis.com/auth/youtube.readonly",
      "access_type" -> "offline",
      "include_granted_scopes" -> "true",
      "state" -> state
    ) ++ forceVerify.option("prompt" -> "consent")
    "https://accounts.google.com/o/oauth2/v2/auth?" + lila.common.url.queryString(params)

  def oauthFetchChannels(code: String, redirectUri: String): Fu[Map[String, String]] =
    val body = Map(
      "client_id" -> cfg.clientId,
      "client_secret" -> cfg.clientSecret.value,
      "code" -> code,
      "grant_type" -> "authorization_code",
      "redirect_uri" -> redirectUri
    )
    ws.url("https://oauth2.googleapis.com/token")
      .post(body)
      .flatMap: rsp =>
        if rsp.status != 200 then
          fufail(s"YouTube token exchange failed: ${rsp.status} ${lila.log.http(rsp.status, rsp.body)}")
        else
          val accessToken = (rsp.body[JsValue] \ "access_token").as[String]
          ws.url(s"$endpoint/channels")
            .withQueryStringParameters("part" -> "id,snippet", "mine" -> "true")
            .withHttpHeaders("Authorization" -> s"Bearer $accessToken")
            .get()
            .map: chRsp =>
              (chRsp.body[JsValue] \ "items")
                .asOpt[JsArray]
                .so: items =>
                  items.value.iterator
                    .map(it => (it \ "id").as[String] -> (it \ "snippet" \ "title").as[String].trim())
                    .toMap

  private[streamer] def subscribeAll: Funit = cfg.apiKey.value.nonEmpty.so:
    for
      channelIds <- repo.approvedIds("youtube")
      _ <- channelIds.parallelN(8) { channelSubscribe(_, true) }
    yield logger.info(s"YouTubeApi.subscribeAll: done ${channelIds.size}")

  private def onVideo(channelId: String, videoId: String): Funit =
    repo
      .byChannelId(channelId)
      .flatMap:
        case Some(s) =>
          isLiveStream(videoId).map: isLive =>
            // this is the only notification we'll get, so don't filter offline users here.
            if isLive then
              repo.setYoutubePubsubVideo(s.id, videoId)
              logger.info(s"YouTube: LIVE ${s.id} vid:$videoId ch:$channelId")
            else logger.debug(s"YouTube: IGNORED ${s.id} vid:$videoId ch:$channelId")
        case None =>
          fuccess:
            logger.info(s"YouTube: UNAPPROVED vid:$videoId ch:$channelId")

  private def isLiveStream(videoId: String): Fu[Boolean] =
    cfg.apiKey.value.nonEmpty.so(
      ws
        .url(s"$endpoint/videos")
        .withQueryStringParameters(
          "part" -> "snippet",
          "id" -> videoId,
          "key" -> cfg.apiKey.value
        )
        .get()
        .map { rsp =>
          rsp.body[JsValue].validate[Youtube.Result] match
            case JsSuccess(data, _) =>
              data.items.headOption.fold(false): item =>
                item.snippet.liveBroadcastContent == "live" && item.snippet.title.value.toLowerCase
                  .contains(keyword.toLowerCase)
            case JsError(err) =>
              logger.warn(s"YouTube ERROR: ${rsp.status} $err ${rsp.body[String].take(200)}")
              false
        }
    )

  private def asFormBody(params: (String, String)*): String =
    params.map((key, value) => s"$key=${java.net.URLEncoder.encode(value, "UTF-8")}").mkString("&")

object Youtube:
  case class Snippet(
      channelId: String,
      title: Html,
      liveBroadcastContent: String,
      defaultAudioLanguage: Option[String]
  )
  case class Item(id: String, snippet: Snippet)
  case class Result(items: List[Item]):
    def streams(keyword: Stream.Keyword, streamers: List[Streamer]): List[YoutubeStream] =
      items
        .withFilter: item =>
          item.snippet.liveBroadcastContent == "live" &&
            item.snippet.title.value.toLowerCase.contains(keyword.toLowerCase)
        .flatMap: item =>
          streamers
            .find(s => s.youTube.exists(_.channelId == item.snippet.channelId))
            .map:
              YoutubeStream(
                item.snippet.channelId,
                unescapeHtml(item.snippet.title),
                item.id,
                _,
                item.snippet.defaultAudioLanguage.flatMap(Lang.get) | lila.core.i18n.defaultLang
              )
  case class YoutubeStream(
      channelId: String,
      status: Html,
      videoId: String,
      streamer: Streamer,
      lang: Lang
  ) extends lila.streamer.Stream:
    def platform = "youTube"
    def urls = Stream.Urls(
      embed = _ => s"https://www.youtube-nocookie.com/embed/${videoId}?autoplay=1&disablekb=1&color=white",
      redirect = s"https://www.youtube.com/watch?v=${videoId}"
    )

  case class StreamerWithYoutube(streamer: Streamer, youtube: Streamer.YouTube)

  private given Reads[Snippet] = Json.reads
  private given Reads[Item] = Json.reads
  given Reads[Result] = Json.reads
