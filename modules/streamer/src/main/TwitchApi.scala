package lila.streamer

import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import lila.core.config.Secret

final private class TwitchApi(ws: StandaloneWSClient, config: TwitchConfig)(using Executor):

  import Stream.Twitch
  import Twitch.given

  private val authBase = "https://id.twitch.tv/oauth2"
  private var tmpToken = Secret("init")
  private val helixBase = "https://api.twitch.tv/helix"

  def fetchStreams(
      streamers: List[Streamer],
      page: Int,
      pagination: Option[Twitch.Pagination]
  ): Fu[List[Twitch.TwitchStream]] =
    (config.clientId.nonEmpty && config.secret.value.nonEmpty && page < 10).so:
      val query = List(
        "game_id" -> "743", // chess-1
        "first" -> "100" // max results per page
      ) ::: List(
        pagination.flatMap(_.cursor).map { "after" -> _ }
      ).flatten
      ws.url(config.endpoint)
        .withQueryStringParameters(query*)
        .withHttpHeaders(
          "Client-ID" -> config.clientId,
          "Authorization" -> s"Bearer ${tmpToken.value}"
        )
        .get()
        .flatMap:
          case res if res.status == 200 =>
            res.body[JsValue].validate[Twitch.Result] match
              case JsSuccess(result, _) => fuccess(result)
              case JsError(err) => fufail(s"twitch $err ${lila.log.http(res.status, res.body)}")
          case res if res.status == 401 && res.body.contains("Invalid OAuth token") =>
            logger.warn("Renewing twitch API token")
            renewToken >> fuccess(Twitch.Result(None, None))
          case res => fufail(s"twitch ${lila.log.http(res.status, res.body)}")
        .recover { case e: Exception =>
          logger.warn(e.getMessage)
          Twitch.Result(None, None)
        }
        .monSuccess(_.tv.streamer.twitch)
        .flatMap { result =>
          if result.data.exists(_.nonEmpty) then
            fetchStreams(streamers, page + 1, result.pagination).map(result.liveStreams ::: _)
          else fuccess(Nil)
        }

  def authorizeUrl(redirectUri: String, state: String, forceVerify: Boolean): String =
    import java.net.URLEncoder.encode
    val params = List(
      "client_id" -> config.clientId,
      "redirect_uri" -> redirectUri,
      "response_type" -> "code",
      "scope" -> "", // ?
      "state" -> state,
      "force_verify" -> forceVerify.toString
    )
    s"$authBase/authorize?" + params.map { case (k, v) => s"$k=${encode(v, "UTF-8")}" }.mkString("&")

  def codeForUser(code: String, redirectUri: String): Fu[(String, String)] =
    val body = Map(
      "client_id" -> config.clientId,
      "client_secret" -> config.secret.value,
      "code" -> code,
      "grant_type" -> "authorization_code",
      "redirect_uri" -> redirectUri
    )
    ws.url(s"$authBase/token").post(body).flatMap { rsp =>
      if rsp.status != 200 then fufail(s"twitch.codeForUser ${lila.log.http(rsp.status, rsp.body)}}")
      else
        val accessToken = (rsp.body[JsValue] \ "access_token").as[String]
        ws.url(s"$helixBase/users")
          .withHttpHeaders(
            "Client-ID" -> config.clientId,
            "Authorization" -> s"Bearer $accessToken"
          )
          .get()
          .flatMap { userRsp =>
            if userRsp.status != 200 then
              fufail(s"twitch.codeForUser ${lila.log.http(rsp.status, rsp.body)}}")
            else
              val data = (userRsp.body[JsValue] \ "data")(0)
              val id = (data \ "id").as[String]
              val login = (data \ "login").as[String]
              fuccess(id -> login)
          }
    }
  private def renewToken: Funit =
    ws.url("https://id.twitch.tv/oauth2/token")
      .withQueryStringParameters(
        "client_id" -> config.clientId,
        "client_secret" -> config.secret.value,
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
