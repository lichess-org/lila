package lila.streamer

import play.api.i18n.Lang
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.JsonBodyWritables.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.Headers
import scala.collection.concurrent.TrieMap

import lila.common.Json.given
import lila.core.config.Secret
import lila.core.config.NetConfig
import lila.core.data.Html

private[streamer] object Twitch:
  case class HelixStream(user_id: String, user_login: String, title: Html, language: String, `type`: String)
  case class Pagination(cursor: Option[String])
  case class Result(data: Option[List[HelixStream]], pagination: Option[Pagination]):
    def liveStreams = (~data).filter(_.`type` == "live")
  case class TwitchStream(id: String, login: String, status: Html, streamer: Streamer, lang: Lang)
      extends lila.streamer.Stream:
    def platform = "twitch"
    def urls = Stream.Urls(
      embed = parent => s"https://player.twitch.tv/?channel=${login}&parent=${parent}",
      redirect = s"https://www.twitch.tv/${login}"
    )
  object TwitchStream:
    def apply(helix: HelixStream, streamer: Streamer): TwitchStream =
      TwitchStream(
        id = helix.user_id,
        login = helix.user_login,
        streamer = streamer,
        status = helix.title,
        lang = Lang.get(helix.language) | lila.core.i18n.defaultLang
      )
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

final private class TwitchApi(ws: StandaloneWSClient, repo: StreamerRepo, cfg: TwitchConfig, net: NetConfig)(
    using Executor
):

  import Twitch.{ given, * }

  private val webhook = s"https://${net.domain}/api/streamer/twitch-eventsub"
  private val eventSubEndpoint = s"${cfg.helixEndpoint}/eventsub/subscriptions"
  private val eventVersions =
    Map("stream.online" -> "1", "stream.offline" -> "1", "channel.update" -> "2")
  private var tmpToken = Secret("init")
  private val lives = TrieMap.empty[String, HelixStream]

  private case class EventSub(subId: String, broadcasterId: String, event: String)

  def liveMatching(
      streamers: List[Streamer],
      filter: (s: TwitchStream) => Boolean
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
    verifyMessage(rawBody, headers).fold(fuccess(none)): messageType =>
      val js = Json.parse(rawBody)
      messageType match
        case "webhook_callback_verification" => fuccess((js \ "challenge").asOpt[String])
        case "notification" =>
          for
            event <- (js \ "event").asOpt[JsObject]
            login <- (event \ "broadcaster_user_login").asOpt[String]
            id <- (event \ "broadcaster_user_id").asOpt[String]
            subType <- (js \ "subscription" \ "type").asOpt[String]
          do
            subType match
              case "stream.online" => fetchStream(id).map(_.foreach(l => lives.update(l.user_id, l)))
              case "stream.offline" => lives.remove(id)
              case "channel.update" =>
                val title = ~(event \ "title").asOpt[String]
                val lang = (event \ "language").asOpt[String].filter(_.nonEmpty).getOrElse("en")
                lives.updateWith(id)(_.map(_.copy(user_login = login, title = Html(title), language = lang)))
              case _ => ()
          fuccess(none)
        case _ => fuccess(none)

  private[streamer] def subscribeAll: Funit = cfg.clientId.nonEmpty.so:
    for
      ids <- repo.approvedIds("twitch")
      subs <- listSubs(Set.empty, none)
      existing = subs.map(sub => (sub.broadcasterId, sub.event)).toSet
      approved = ids.toSet
      wanted = ids.flatMap: id =>
        eventVersions.keys
          .filterNot(event => existing(id, event))
          .map(event => (id, event))
      _ <- deleteSubs(subs.filter { case EventSub(_, id, _) => !approved(id) }.toList)
      _ <- wanted.parallelN(8):
        case (id, event) => subscribeEvent(id, event)
    yield ()

  private[streamer] def syncAll: Funit = cfg.clientId.nonEmpty.so:
    repo
      .approvedIds("twitch")
      .map: ids =>
        ids
          .grouped(100)
          .toList
          .sequentially(fetchStreams)
          .map(_.flatten)
          .foreach: streams =>
            val newLives = streams.iterator.map(l => l.user_id -> l).toMap
            val freshIds = newLives.keySet
            ids.iterator.filterNot(freshIds).foreach(lives.remove)
            newLives.foreach { case (id, live) => lives.update(id, live) }

  private[streamer] def forceCheck(s: Streamer.Twitch): Funit =
    fetchStream(s.id).map(helix => lives.updateWith(s.id)(_ => helix))

  private[streamer] def pubsubSubscribe(id: String, subscribe: Boolean): Funit =
    if subscribe then eventVersions.keys.map(event => subscribeEvent(id, event)).parallel.void
    else fetchStreamSubs(id).map(deleteSubs)

  private def subscribeEvent(id: String, event: String) =
    val body = Json
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
    ensureToken() >>
      ws.url(eventSubEndpoint)
        .withHttpHeaders(headersAuth*)
        .post(body)

  private def fetchStream(id: String): Fu[Option[HelixStream]] =
    fetchStreams(Seq(id)).map(_.headOption)

  private def fetchStreamSubs(id: String): Fu[Seq[EventSub]] =
    ws.url(eventSubEndpoint)
      .withQueryStringParameters("user_id" -> id)
      .withHttpHeaders(headersAuth*)
      .get()
      .map: res =>
        val js = res.body[JsValue]
        val data = (js \ "data").asOpt[List[JsValue]].getOrElse(Nil)
        data.flatMap: sub =>
          for
            subId <- (sub \ "id").asOpt[String]
            event <- (sub \ "type").asOpt[String]
            hook <- (sub \ "transport" \ "callback").asOpt[String]
            if hook == webhook
          yield EventSub(subId, id, event)

  private def listSubs(soFar: Set[EventSub], after: Option[String]): Fu[Set[EventSub]] =
    val request = ws.url(eventSubEndpoint).withHttpHeaders(headersAuth*)
    ensureToken() >>
      after
        .fold(request)(cursor => request.withQueryStringParameters("after" -> cursor))
        .get()
        .flatMap: res =>
          val js = res.body[JsValue]
          val data = (js \ "data").asOpt[List[JsValue]].getOrElse(Nil)
          val pageSet = data.flatMap: d =>
            for
              subId <- (d \ "id").asOpt[String]
              broadcasterId <- (d \ "condition" \ "broadcaster_user_id").asOpt[String]
              event <- (d \ "type").asOpt[String]
              hook <- (d \ "transport" \ "callback").asOpt[String]
              if hook == webhook
            yield EventSub(subId, broadcasterId, event)

          val result = soFar ++ pageSet.toSet
          (js \ "pagination" \ "cursor")
            .asOpt[String]
            .fold(fuccess(result))(after => listSubs(result, after.some))

  private def deleteSubs(subs: Seq[EventSub]): Funit =
    ensureToken() >>
      subs.parallelN(8):
        case EventSub(subId, _, _) =>
          ws.url(s"$eventSubEndpoint?id=$subId")
            .withHttpHeaders(headersAuth*)
            .delete()
            .void

  private def fetchStreams(ids: Seq[String]): Fu[Seq[HelixStream]] =
    if ids.isEmpty then fuccess(Nil)
    else
      ensureToken() >>
        ws.url(s"${cfg.helixEndpoint}/streams")
          .withQueryStringParameters(ids.map(l => "user_id" -> l)*)
          .withHttpHeaders(headersAuth*)
          .get()
          .map: res =>
            val data = (res.body[JsValue] \ "data").asOpt[Seq[JsValue]].getOrElse(Nil)
            data.flatMap: d =>
              if (d \ "type").asOpt[String].contains("live") then d.asOpt[HelixStream]
              else none

  private def verifyMessage(rawBody: String, headers: Headers): Option[String] =
    def header(name: String): Option[String] = headers.get(s"Twitch-Eventsub-Message-$name")

    val expected = javax.crypto.Mac.getInstance("HmacSHA256")
    expected.init(new javax.crypto.spec.SecretKeySpec(cfg.secret.value.getBytes("UTF-8"), "HmacSHA256"))
    val mac = expected
      .doFinal(((header("Id") ++ header("Timestamp")).mkString + rawBody).getBytes())
      .map("%02x".format(_))
      .mkString
    if header("Signature").exists(_.equalsIgnoreCase(s"sha256=$mac")) then header("Type")
    else none

  private def ensureToken(): Funit =
    if tmpToken.value == "init" then renewToken() else funit

  private def renewToken(): Funit =
    ws.url(s"${cfg.authEndpoint}/token")
      .withQueryStringParameters(
        "client_id" -> cfg.clientId,
        "client_secret" -> cfg.secret.value,
        "grant_type" -> "client_credentials"
      )
      .post(Map.empty[String, String])
      .flatMap:
        case res if res.status == 200 =>
          res.body[JsValue].asOpt[JsObject].flatMap(_.str("access_token")) match
            case Some(token) =>
              tmpToken = Secret(token)
              funit
            case _ => fufail(s"twitch.renewToken ${lila.log.http(res.status, res.body)}")
        case res => fufail(s"twitch.renewToken ${lila.log.http(res.status, res.body)}")

  private def headersAuth =
    Seq(
      "Client-ID" -> cfg.clientId,
      "Authorization" -> s"Bearer ${tmpToken.value}",
      "Content-Type" -> "application/json"
    )
