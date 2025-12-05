package lila.streamer

import play.api.mvc.{ RequestHeader, DiscardingCookie }
import com.github.blemale.scaffeine.Scaffeine
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.json.*

/* let lichess users associate their twitch/youtube accounts */
final class StreamerOauth(ws: StandaloneWSClient, youtubeCfg: YoutubeConfig, twitchCfg: TwitchConfig)(using
    Executor
):

  type State = String

  def makeState(): State = scalalib.ThreadLocalRandom.nextString(32)

  object cookie:

    def name(platform: Platform) = s"${platform}_oauth_state"

    def get(platform: Platform)(using req: RequestHeader) =
      req.cookies.get(name(platform)).map(_.value)

    def unset(platform: Platform) = DiscardingCookie(name(platform), path = "/")

  object youtubeChannelCache:

    private val cache = Scaffeine().expireAfterWrite(5.minutes).build[State, (UserId, Map[String, String])]()

    def put(state: State, idsMap: Map[String, String])(using me: Me) =
      if idsMap.nonEmpty then cache.put(state, (me.userId, idsMap))

    def find(channelId: String)(using me: Me)(using RequestHeader): Option[(UserId, Map[String, String])] =
      cookie
        .get("youtube")
        .flatMap(cache.getIfPresent)
        .filter((userId, idsMap) => me.is(userId) && idsMap.contains(channelId))

  object authorizeUrl:

    def twitch(redirectUri: Url, state: String, forceVerify: Boolean): String =
      val params = Map(
        "client_id" -> twitchCfg.clientId,
        "redirect_uri" -> redirectUri.value,
        "response_type" -> "code",
        "scope" -> "", // ?
        "state" -> state,
        "force_verify" -> forceVerify.toString
      )
      s"${twitchCfg.authEndpoint}/authorize?" + lila.common.url.queryString(params)

    def youtube(redirectUri: Url, state: State, forceVerify: Boolean): String =
      val params = Map(
        "client_id" -> youtubeCfg.clientId,
        "redirect_uri" -> redirectUri.value,
        "response_type" -> "code",
        "scope" -> "https://www.googleapis.com/auth/youtube.readonly",
        "access_type" -> "offline",
        "include_granted_scopes" -> "true",
        "state" -> state
      ) ++ forceVerify.option("prompt" -> "consent")
      "https://accounts.google.com/o/oauth2/v2/auth?" + lila.common.url.queryString(params)

  def twitchUser(code: String, redirectUri: Url): Fu[Streamer.Twitch] =
    val body = Map(
      "client_id" -> twitchCfg.clientId,
      "client_secret" -> twitchCfg.secret.value,
      "code" -> code,
      "grant_type" -> "authorization_code",
      "redirect_uri" -> redirectUri.value
    )
    ws.url(s"${twitchCfg.authEndpoint}/token")
      .post(body)
      .flatMap: rsp =>
        if rsp.status != 200 then fufail(s"twitch.codeForUser ${lila.log.http(rsp.status, rsp.body)}}")
        else
          val accessToken = (rsp.body[JsValue] \ "access_token").as[String]
          ws.url(s"${twitchCfg.helixEndpoint}/users")
            .withHttpHeaders(
              "Client-ID" -> twitchCfg.clientId,
              "Authorization" -> s"Bearer $accessToken"
            )
            .get()
            .flatMap: userRsp =>
              if userRsp.status != 200
              then fufail(s"twitch.codeForUser ${lila.log.http(rsp.status, rsp.body)}}")
              else
                userRsp
                  .body[JsValue]
                  .arr("data")
                  .flatMap(_.value.headOption)
                  .flatMap(_.asOpt[Streamer.Twitch])
                  .fold(fufail[Streamer.Twitch](s"twitch.codeForUser no user in response"))(fuccess)

  def youtubeChannels(code: String, redirectUri: Url): Fu[Map[String, String]] =
    val body = Map(
      "client_id" -> youtubeCfg.clientId,
      "client_secret" -> youtubeCfg.clientSecret.value,
      "code" -> code,
      "grant_type" -> "authorization_code",
      "redirect_uri" -> redirectUri.value
    )
    ws.url("https://oauth2.googleapis.com/token")
      .post(body)
      .flatMap: rsp =>
        if rsp.status != 200 then
          fufail(s"Youtube token exchange failed: ${rsp.status} ${lila.log.http(rsp.status, rsp.body)}")
        else
          val accessToken = (rsp.body[JsValue] \ "access_token").as[String]
          ws.url(s"${youtubeCfg.v3Endpoint}/channels")
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
