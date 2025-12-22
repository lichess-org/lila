package lila.streamer

import scala.collection.concurrent.TrieMap
import akka.stream.scaladsl.*
import play.api.i18n.Lang
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.{ StandaloneWSClient, StandaloneWSResponse }
import play.api.mvc.Headers

import lila.common.Json.given
import lila.core.config.Secret
import lila.core.config.NetConfig
import lila.core.data.Html

private object Twitch:

  opaque type TwitchId = String
  object TwitchId extends OpaqueString[TwitchId]

  opaque type TwitchLogin = String
  object TwitchLogin extends OpaqueString[TwitchLogin]

  case class HelixStream(
      user_id: TwitchId,
      user_login: TwitchLogin,
      title: Html,
      language: String,
      `type`: String
  ):
    def live = `type` == "live"
  case class Pagination(cursor: Option[String])
  case class Result(data: Option[List[HelixStream]], pagination: Option[Pagination]):
    def liveStreams = (~data).filter(_.live)
  case class TwitchStream(stream: HelixStream, streamer: Streamer) extends lila.streamer.Stream:
    def platform = "twitch"
    def status = stream.title
    def urls = lila.streamer.Stream.Urls(
      embed = parent => s"https://player.twitch.tv/?channel=${stream.user_login}&parent=${parent}",
      redirect = s"https://www.twitch.tv/${stream.user_login}"
    )
    def lang = Lang.get(stream.language) | lila.core.i18n.defaultLang
  given Reads[HelixStream] = Json.reads
  given Reads[Result] = Json.reads
  given Reads[Pagination] = Json.reads

private class TwitchConfig(
    val endpoint: String,
    @lila.common.autoconfig.ConfigName("client_id") val clientId: String,
    val secret: Secret
):
  val authEndpoint = "https://id.twitch.tv/oauth2"
  val helixEndpoint = "https://api.twitch.tv/helix"

final private class TwitchApi(
    ws: StandaloneWSClient,
    repo: StreamerRepo,
    cfg: TwitchConfig,
    net: NetConfig,
    cacheApi: lila.memo.CacheApi
)(using Executor, akka.stream.Materializer):

  import Twitch.{ given, * }

  private val logger = lila.streamer.logger.branch("twitch")
  private val webhook = net.routeUrl(routes.Streamer.onTwitchEventSub)
  private val eventSubEndpoint = s"${cfg.helixEndpoint}/eventsub/subscriptions"
  private val eventVersions = Map("stream.online" -> "1", "stream.offline" -> "1", "channel.update" -> "2")
  private val reqsPerMinute = 30
  private val lives = TrieMap.empty[TwitchId, HelixStream]

  private case class EventSub(subId: String, broadcasterId: TwitchId, event: String, hook: Url)

  def debugLives: String = lives.toString

  def liveMatching(
      streamers: List[Streamer],
      filter: TwitchStream => Boolean
  ): Fu[List[TwitchStream]] =
    fuccess:
      val ids = streamers.flatMap(_.twitch.map(_.id)).toSet
      lives.valuesIterator
        .filter(live => ids(live.user_id))
        .toList
        .flatMap: helix =>
          streamers
            .find: s =>
              (s.twitch.exists(_.id == helix.user_id)) && filter(TwitchStream(helix, s))
            .map: streamer =>
              if streamer.twitch.exists(_.login != helix.user_login)
              then discard { repo.setTwitchLogin(streamer.id, helix.user_login) } // rare
              Twitch.TwitchStream(helix, streamer)

  def onMessage(rawBody: String, headers: Headers): Fu[Option[String]] =
    verifyMessage(rawBody, headers).so: messageType =>
      val js = Json.parse(rawBody)
      messageType match
        case "webhook_callback_verification" => fuccess((js \ "challenge").asOpt[String])
        case "notification" =>
          val done = for
            event <- (js \ "event").asOpt[JsObject]
            login <- (event \ "broadcaster_user_login").asOpt[TwitchLogin]
            id <- (event \ "broadcaster_user_id").asOpt[TwitchId]
            subType <- (js \ "subscription" \ "type").asOpt[String]
          yield subType match
            case "stream.online" =>
              logger.info(s"stream online: $login ($id) exists: ${lives.contains(id)})")
              fetchStream(id).map(_.foreach(l => lives.update(l.user_id, l)))
            case "stream.offline" =>
              logger.info(s"stream offline: $login ($id) exists: ${lives.contains(id)})")
              lives.remove(id)
            case "channel.update" =>
              val title = ~(event \ "title").asOpt[String]
              val lang = (event \ "language").asOpt[String].filter(_.nonEmpty).getOrElse("en")
              logger.info(s"channel update: $login ($id) title: $title lang: $lang")
              lives.updateWith(id)(_.map(_.copy(user_login = login, title = Html(title), language = lang)))
            case _ => ()
          if done.isEmpty then logger.warn(s"Unknown Twitch event notification: $js")
          fuccess(none)
        case _ => fuccess(none)

  private[streamer] def subscribeAll: Funit =
    for
      latestSeenApprovedIds <- repo.approvedTwitchIds()
      _ = logger.info(s"${latestSeenApprovedIds.size} approved twitch ids")
      allSubs <- listSubs(Set.empty, none)
      _ = logger.info(s"Currently subscribed to ${allSubs.size} event subs")
      (invalid, subs) = allSubs.partition(_.hook != webhook)
      _ <- invalid.nonEmpty.so:
        logger.info(s"Deleting ${invalid.size} invalid event subs")
        deleteSubs(invalid.toList)
      _ = logger.info(s"Currently subscribed to ${subs.size} valid event subs")
      existing = subs.map(sub => (sub.broadcasterId, sub.event)).toSet
      approved = latestSeenApprovedIds.toSet
      wanted = latestSeenApprovedIds.flatMap: id =>
        eventVersions.keys.filterNot(event => existing(id -> event)).map(id -> _)
      subsToDelete = subs.filter(s => !approved(s.broadcasterId)).toList
      _ <- deleteSubs(subsToDelete)
      _ = logger.info(s"Subscribing to ${wanted.size} new event subs")
      _ <- subscribeMany(wanted)
    yield ()

  private[streamer] def syncAll: Funit =
    for
      latestSeenApprovedIds <- repo.approvedTwitchIds()
      allOngoingStreams <- latestSeenApprovedIds
        .grouped(100)
        .toList
        .sequentially(fetchStreams)
        .map(_.flatten)
      newLives = allOngoingStreams.view.map(l => l.user_id -> l).toMap
    yield
      (lives.keySet.toSet -- newLives.keySet).foreach(lives.remove)
      newLives.foreach(lives.update)

  private[streamer] def checkThatLiveStreamersReallyAreLive: Funit =
    fetchStreams(lives.keys.toSeq).map { helixes =>
      val liveIds = helixes.map(_.user_id).toSet
      lives.foreach: (id, stream) =>
        if !liveIds(id) then
          logger.info(s"Stream ${id}/${stream.user_login} seems offline, removing from lives")
          lives.remove(id)
    }

  private[streamer] def forceCheck(s: Streamer.Twitch): Funit =
    fetchStream(s.id).map(helix => lives.updateWith(s.id)(_ => helix))

  private[streamer] def pubsubSubscribe(id: TwitchId, subscribe: Boolean): Funit =
    if subscribe
    then streamRequests(eventVersions.keys.toList)("subscribe")(subscribeEvent(id, _))
    else fetchStreamSubs(id).map(deleteSubs)

  private def subscribeMany(wanted: Seq[(TwitchId, String)]): Funit =
    streamRequests(wanted.toList)("subscribe")(subscribeEvent)

  private def subscribeEvent(id: TwitchId, event: String) =
    for
      headers <- headersAuth
      body = Json
        .obj(
          "type" -> event,
          "condition" -> Json.obj("broadcaster_user_id" -> id),
          "transport" -> Json.obj(
            "method" -> "webhook",
            "callback" -> webhook,
            "secret" -> cfg.secret.value
          )
        )
        .add("version" -> eventVersions.get(event))
      res <- ws
        .url(eventSubEndpoint)
        .withHttpHeaders(headers*)
        .post(body)
        .addEffect(logFailed(s"subscribeEvent $id $event"))
    yield res

  private def fetchStream(id: TwitchId): Fu[Option[HelixStream]] =
    fetchStreams(Seq(id)).map(_.headOption)

  private def fetchStreamSubs(id: TwitchId): Fu[Seq[EventSub]] =
    for
      headers <- headersAuth
      res <- ws
        .url(eventSubEndpoint)
        .withQueryStringParameters("user_id" -> id.value)
        .withHttpHeaders(headers*)
        .get()
        .addEffect(logFailed(s"fetchStreamSubs $id"))
    yield
      for
        sub <- ~res.body[JsValue].get[List[JsValue]]("data")
        subId <- (sub \ "id").asOpt[String]
        event <- (sub \ "type").asOpt[String]
        hook <- (sub \ "transport" \ "callback").asOpt[Url]
      yield EventSub(subId, id, event, hook)

  private def listSubs(soFar: Set[EventSub], after: Option[String]): Fu[Set[EventSub]] =
    logger.info(s"Listing subs, so far ${soFar.size}")
    for
      headers <- headersAuth
      request = ws.url(eventSubEndpoint).withHttpHeaders(headers*)
      res <- after
        .fold(request)(cursor => request.withQueryStringParameters("after" -> cursor))
        .get()
        .addEffect(logFailed(s"listSubs (so far: ${soFar.size})"))
      subs <-
        val js = res.body[JsValue]
        val pageSet = for
          d <- ~js.get[List[JsValue]]("data")
          subId <- d.str("id")
          broadcasterId <- (d \ "condition" \ "broadcaster_user_id").asOpt[TwitchId]
          event <- d.str("type")
          hook <- (d \ "transport" \ "callback").asOpt[Url]
        yield EventSub(subId, broadcasterId, event, hook)

        val result = soFar ++ pageSet.toSet
        (js \ "pagination" \ "cursor")
          .asOpt[String]
          .fold(fuccess(result))(after => listSubs(result, after.some))
    yield subs

  private def deleteSubs(subs: Seq[EventSub]): Funit =
    for
      headers <- headersAuth
      _ <- streamRequests(subs.toList)("unsubscribe"): sub =>
        ws.url(s"$eventSubEndpoint?id=${sub.subId}")
          .withHttpHeaders(headers*)
          .delete()
          .addEffect(logFailed(s"deleteSub ${sub.subId}"))
    yield ()

  private def streamRequests[A](list: List[A])(msg: String)(send: A => Fu[StandaloneWSResponse]): Funit =
    val size = list.size
    Source(list)
      .throttle(reqsPerMinute * 9 / 10, 1.minute)
      .mapAsync(1)(send)
      .map(_ => ())
      .runWith:
        Sink.fold[Int, Unit](0): (counter, _) =>
          if counter % 20 == 0 then logger.info(s"$counter/$size $msg")
          counter + 1
      .void

  private def logFailed(context: String)(res: StandaloneWSResponse) =
    if res.status / 100 != 2 then logger.warn(s"$context failed: ${lila.log.http(res.status, res.body)}")

  private def fetchStreams(ids: Seq[TwitchId]): Fu[Seq[HelixStream]] =
    ids.nonEmpty.so:
      for
        headers <- headersAuth
        res <- ws
          .url(s"${cfg.helixEndpoint}/streams")
          .withQueryStringParameters(ids.map(l => "user_id" -> l.value)*)
          .withHttpHeaders(headers*)
          .get()
          .addEffect(logFailed(s"fetchStreams ${ids.mkString(",")}"))
      yield res.body[JsValue].get[Seq[HelixStream]]("data").orZero.filter(_.live)

  private def verifyMessage(rawBody: String, headers: Headers): Option[String] =
    def header(name: String): Option[String] = headers.get(s"Twitch-Eventsub-Message-$name")

    val expected = javax.crypto.Mac.getInstance("HmacSHA256")
    expected.init(new javax.crypto.spec.SecretKeySpec(cfg.secret.value.getBytes("UTF-8"), "HmacSHA256"))
    val mac = expected
      .doFinal(((header("Id") ++ header("Timestamp")).mkString + rawBody).getBytes())
      .map("%02x".format(_))
      .mkString
    header("Signature").exists(_.equalsIgnoreCase(s"sha256=$mac")) so header("Type")

  private object bearerToken:

    private val cache = cacheApi.unit[Secret]:
      _.refreshAfterWrite(55.minutes).buildAsyncFuture: _ =>
        renewToken()

    def get: Fu[Secret] = cache.get({})

    private def renewToken(): Fu[Secret] =
      ws.url(s"${cfg.authEndpoint}/token")
        .withQueryStringParameters(
          "client_id" -> cfg.clientId,
          "client_secret" -> cfg.secret.value,
          "grant_type" -> "client_credentials"
        )
        .post(Map.empty[String, String])
        .flatMap:
          case res if res.status == 200 =>
            res.body[JsValue].str("access_token") match
              case Some(token) =>
                logger.info("token renewed")
                fuccess(Secret(token))
              case _ => fufail(s"twitch.renewToken ${lila.log.http(res.status, res.body)}")
          case res => fufail(s"twitch.renewToken ${lila.log.http(res.status, res.body)}")

  private def headersAuth = bearerToken.get.map { tmpToken =>
    Seq(
      "Client-ID" -> cfg.clientId,
      "Authorization" -> s"Bearer ${tmpToken.value}",
      "Content-Type" -> "application/json"
    )
  }
